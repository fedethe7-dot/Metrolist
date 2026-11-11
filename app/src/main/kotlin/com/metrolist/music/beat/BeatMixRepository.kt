/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.beat

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.constants.DislikedSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.metrolist.music.ui.utils.resize
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.random.Random

/** One song in a mix; [isNew] means never listened to before. */
data class BeatMixTrack(
    val metadata: MediaMetadata,
    val isNew: Boolean,
)

data class BeatMixContent(
    val def: BeatMixDef,
    val tracks: List<BeatMixTrack>,
    val builtAt: Long,
) {
    val newCount: Int get() = tracks.count { it.isNew }
}

/**
 * Builds Beat's mixes: decides which mixes to propose (from the listening history) and fills each one
 * with songs the user knows and songs found on YouTube Music, different every time it is built.
 */
@Singleton
class BeatMixRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) {
        val memory = BeatMixMemory(File(context.filesDir, "beat_mix_memory.json"))

        private val _lineup = MutableStateFlow<BeatMixLineup?>(null)
        val lineup: StateFlow<BeatMixLineup?> = _lineup.asStateFlow()

        private val lineupMutex = Mutex()
        private val tasteMutex = Mutex()
        private var tasteCache: Taste? = null
        private val contents = ConcurrentHashMap<String, BeatMixContent>()
        private val buildLocks = ConcurrentHashMap<String, Mutex>()
        private val covers = ConcurrentHashMap<String, List<String>>()
        private val coverSearches = Semaphore(3)

        private val _coverVersion = MutableStateFlow(0)

        /** Changes every time covers are renewed, so cards on screen reload them. */
        val coverVersion: StateFlow<Int> = _coverVersion.asStateFlow()

        private class Taste(
            val builtAt: Long,
            val profile: BeatTasteProfile,
            /** Songs the user listened to or liked, most listened first. */
            val knownPool: List<Song>,
            val forgotten: List<Song>,
            val heavyRotation: Set<String>,
            /** Normalized artist name to photo. */
            val artistPhotos: Map<String, String>,
        )

        /** Makes sure today's mixes are planned. [rotate] asks for a different selection ("Cambia i mix"). */
        suspend fun loadLineup(rotate: Boolean = false): BeatMixLineup =
            lineupMutex.withLock {
                val day = beatDayNumber()
                val current = _lineup.value
                if (!rotate && current != null && current.day == day) return current
                val rotation = withContext(Dispatchers.IO) { if (rotate) memory.nextRotation() else memory.rotation() }
                val taste = taste(force = rotate)
                val planned =
                    BeatMixPlanner.plan(
                        profile = taste.profile,
                        day = day,
                        rotation = rotation,
                        hasRediscover = taste.forgotten.size >= 5 || taste.knownPool.size >= 60,
                    )
                if (rotate || current?.day != day) {
                    covers.clear()
                    _coverVersion.value++
                }
                _lineup.value = planned
                planned
            }

        suspend fun findDef(id: String): BeatMixDef? =
            contents[id]?.def
                ?: (_lineup.value ?: loadLineup()).find(id)
                ?: loadLineup().find(id)
                ?: BeatMixPlanner.fromId(id, taste().profile)

        fun cached(id: String): BeatMixContent? = contents[id]?.takeIf { System.currentTimeMillis() - it.builtAt < CONTENT_TTL_MS }

        /** Songs of a mix; [refresh] builds a brand new selection. */
        suspend fun content(
            def: BeatMixDef,
            refresh: Boolean = false,
        ): BeatMixContent {
            if (!refresh) cached(def.id)?.let { return it }
            val lock = buildLocks.getOrPut(def.id) { Mutex() }
            return lock.withLock {
                if (!refresh) cached(def.id)?.let { return@withLock it }
                val built = withContext(Dispatchers.IO) { build(def) }
                contents[def.id] = built
                if (refresh || def.id !in covers) {
                    covers[def.id] = coverFromTracks(def, built)
                    _coverVersion.value++
                }
                built
            }
        }

        private suspend fun taste(force: Boolean = false): Taste =
            tasteMutex.withLock {
                val now = System.currentTimeMillis()
                tasteCache?.takeIf { !force && now - it.builtAt < TASTE_TTL_MS }?.let { return it }
                withContext(Dispatchers.IO) {
                    val today = LocalDateTime.now()
                    val recent = database.mostPlayedArtists(today.minusDays(30), limit = 80).first()
                    val year = database.mostPlayedArtists(today.minusDays(365), limit = 150).first()
                    val recentByName = recent.associate { normalizeArtist(it.artist.name) to (it.timeListened ?: 0).toLong() }
                    val listens =
                        (year.map { it.artist.name } + recent.map { it.artist.name })
                            .distinctBy(::normalizeArtist)
                            .map { name ->
                                val key = normalizeArtist(name)
                                val total = year.firstOrNull { normalizeArtist(it.artist.name) == key }?.timeListened?.toLong() ?: 0L
                                val recentMs = recentByName[key] ?: 0L
                                BeatArtistListen(name, recentMs, maxOf(total, recentMs))
                            }

                    val liked = database.likedSongsByNameAsc().first()
                    val likedByArtist =
                        liked
                            .flatMap { song -> song.artists.map { normalizeArtist(it.name) } }
                            .filter { it.isNotEmpty() }
                            .groupingBy { it }
                            .eachCount()
                    val displayNames =
                        liked.flatMap { it.artists }.associate { normalizeArtist(it.name) to it.name }

                    val knownPool =
                        (database.mostPlayedSongs(today.minusDays(730), limit = 600).first() + liked)
                            .distinctBy { it.id }
                    val heavy = database.mostPlayedSongs(today.minusDays(14), limit = 40).first().map { it.id }.toSet()
                    val forgotten = database.forgottenFavorites().first()

                    val profile =
                        BeatTasteProfile.build(
                            listens = listens,
                            likedByArtist = likedByArtist,
                            skipsByArtist = memory.artistSkips(),
                            learnedGenres = memory.learnedGenres(),
                            displayNames = displayNames,
                        )
                    val artistPhotos = HashMap<String, String>()
                    (year + recent).forEach { artist ->
                        artist.artist.thumbnailUrl?.let { artistPhotos[normalizeArtist(artist.artist.name)] = it }
                    }
                    knownPool.flatMap { it.artists }.forEach { artist ->
                        artist.thumbnailUrl?.let { artistPhotos.putIfAbsent(normalizeArtist(artist.name), it) }
                    }
                    Taste(now, profile, knownPool, forgotten, heavy, artistPhotos)
                }.also { tasteCache = it }
            }

        private suspend fun build(def: BeatMixDef): BeatMixContent {
            val taste = taste()
            val random = Random(System.nanoTime())
            val profile = taste.profile
            val genre = BeatMixCatalog.genre(def.genreId)
            val knownIds = taste.knownPool.map { it.id }.toHashSet()

            // Songs found online (mostly new), with their metadata.
            val online = LinkedHashMap<String, SongItem>()
            // Songs the user knows, with a relevance for this mix.
            val known = LinkedHashMap<String, Pair<Song, Double>>()
            var maxPerArtist = 4
            var excludedArtists = emptySet<String>()
            var newArtistBoost = 1.0

            fun addKnown(
                songs: List<Song>,
                relevance: (Song) -> Double,
            ) = songs.forEach { song -> if (song.id !in known) known[song.id] = song to relevance(song) }

            fun playRelevance(song: Song): Double = 0.6 + ln(1.0 + song.song.totalPlayTime / 180_000.0) * 0.4 + if (song.song.liked) 0.4 else 0.0

            fun songsBy(artists: Collection<String>): List<Song> =
                taste.knownPool.filter { song -> song.artists.any { normalizeArtist(it.name) in artists } }

            when (def.kind) {
                BeatMixKind.GENRE, BeatMixKind.ORIGINAL, BeatMixKind.OUTSIDE -> {
                    val genreId = genre?.id
                    val genreArtists =
                        (genre?.artists.orEmpty() + profile.artistGenres.filterValues { genreId != null && genreId in it }.keys).toSet()
                    val userArtists = profile.topArtists(200).filter { it in genreArtists }
                    val queries =
                        genre?.seeds.orEmpty().shuffled(random).take(2) +
                            listOfNotNull(
                                (userArtists.shuffled(random).firstOrNull() ?: genre?.artists?.randomOrNull(random))
                                    ?.let { profile.displayName(it) },
                            )
                    radios(queries.map { Seed.Query(it) }).forEach { online.putIfAbsent(it.id, it) }
                    val radioArtists = online.values.flatMap { it.artists.map { a -> normalizeArtist(a.name) } }.toSet()
                    addKnown(songsBy(genreArtists)) { playRelevance(it) * 1.3 }
                    if (def.kind != BeatMixKind.OUTSIDE) addKnown(songsBy(radioArtists)) { playRelevance(it) * 0.8 }
                }

                BeatMixKind.DAILY, BeatMixKind.ARTIST -> {
                    val artists = def.artists.map(::normalizeArtist).toSet()
                    val theirSongs = songsBy(artists)
                    val seeds =
                        artists.shuffled(random).take(3).map { artist ->
                            theirSongs
                                .filter { song -> song.artists.any { normalizeArtist(it.name) == artist } }
                                .shuffled(random)
                                .firstOrNull()
                                ?.let { Seed.Track(it.id) }
                                ?: Seed.Query(profile.displayName(artist))
                        }.let { if (def.kind == BeatMixKind.ARTIST) it + Seed.Query(def.artists.first()) else it }
                    radios(seeds).forEach { online.putIfAbsent(it.id, it) }
                    addKnown(theirSongs) { playRelevance(it) * 1.4 }
                    if (def.kind == BeatMixKind.ARTIST) {
                        maxPerArtist = 18
                        // The artist's own catalogue, not only what the radio proposes.
                        search(def.artists.first(), 12).forEach { online.putIfAbsent(it.id, it) }
                    } else {
                        val radioArtists = online.values.flatMap { it.artists.map { a -> normalizeArtist(a.name) } }.toSet()
                        addKnown(songsBy(radioArtists)) { playRelevance(it) * 0.7 }
                    }
                }

                BeatMixKind.SIMILAR -> {
                    val artist = def.artists.firstOrNull().orEmpty()
                    val key = normalizeArtist(artist)
                    excludedArtists = setOf(key)
                    val seedId =
                        songsBy(setOf(key)).maxByOrNull { it.song.totalPlayTime }?.id
                            ?: search(artist, 1).firstOrNull()?.id
                    val similar = seedId?.let { similarArtists(it) }.orEmpty().filter { normalizeArtist(it) != key }
                    val picked = similar.shuffled(random).take(5)
                    coroutineScope {
                        picked.map { name -> async { search(name, 5) } }.awaitAll()
                    }.flatten().forEach { online.putIfAbsent(it.id, it) }
                    val radioSeeds: List<Seed> = picked.take(2).map<String, Seed> { Seed.Query(it) }.ifEmpty { listOfNotNull(seedId?.let { Seed.Track(it) }) }
                    radios(radioSeeds).forEach { online.putIfAbsent(it.id, it) }
                    addKnown(songsBy(similar.map(::normalizeArtist).toSet())) { playRelevance(it) }
                }

                BeatMixKind.DISCOVER -> {
                    newArtistBoost = 2.5
                    val seeds =
                        weightedSample(taste.knownPool.take(150), 4, random) { playRelevance(it) }
                            .map { Seed.Track(it.id) }
                    radios(seeds).forEach { online.putIfAbsent(it.id, it) }
                }

                BeatMixKind.REDISCOVER -> {
                    val oldLiked =
                        taste.knownPool.filter { song ->
                            song.id !in taste.heavyRotation &&
                                song.song.liked &&
                                (song.song.likedDate?.isBefore(LocalDateTime.now().minusDays(90)) ?: true)
                        }
                    addKnown(taste.forgotten) { playRelevance(it) * 1.5 }
                    addKnown(oldLiked) { playRelevance(it) }
                    val seed = (taste.forgotten.ifEmpty { oldLiked }).randomOrNull(random)
                    radios(listOfNotNull(seed?.let { Seed.Track(it.id) })).forEach { online.putIfAbsent(it.id, it) }
                }
            }

            // Online songs already in the library or history count as known, not new.
            val onlineKnown =
                runCatching {
                    database.getSongsByIds(online.keys.filter { it !in knownIds && it !in known }.toList())
                        .filter { it.song.totalPlayTime > 0 || it.song.liked }
                        .map { it.id }
                        .toHashSet()
                }.getOrDefault(hashSetOf())

            val candidates = ArrayList<BeatCandidate>()
            val metadata = HashMap<String, MediaMetadata>()
            online.values
                .filter { !it.isEpisode && it.artists.none { a -> normalizeArtist(a.name) in excludedArtists } }
                .forEach { item ->
                    val artists = item.artists.map { normalizeArtist(it.name) }
                    val isKnown = item.id in knownIds || item.id in onlineKnown || item.id in known
                    if (def.kind == BeatMixKind.DISCOVER && isKnown) return@forEach
                    val inGenre = genre != null && artists.any { it in genre.artists }
                    val newArtist = artists.none { it in profile.artistScores }
                    candidates +=
                        BeatCandidate(
                            id = item.id,
                            artists = artists,
                            known = isKnown,
                            relevance = (if (inGenre) 1.3 else 1.0) * (if (newArtist) newArtistBoost else 1.0),
                        )
                    metadata[item.id] = item.toMediaMetadata()
                }
            known.values
                .filter { (song, _) -> song.artists.none { normalizeArtist(it.name) in excludedArtists } }
                .forEach { (song, relevance) ->
                    if (song.id in metadata) return@forEach
                    candidates += BeatCandidate(song.id, song.artists.map { normalizeArtist(it.name) }, known = true, relevance = relevance)
                    metadata[song.id] = song.toMediaMetadata()
                }

            val disliked = context.dataStore.data.first()[DislikedSongsKey].orEmpty()
            val state = memory.rotationState(taste.heavyRotation, disliked)
            val selected =
                BeatMixSelector.select(
                    candidates = candidates,
                    newShare = def.kind.newShare,
                    state = state,
                    random = random,
                    maxPerArtist = maxPerArtist,
                )
            val ordered = BeatSmartShuffle.shuffle(selected, random)
            if (ordered.isEmpty()) error("No songs for ${def.id}")

            val genreForMemory = if (def.kind in GenreKinds) def.genreId else null
            memory.recordServed(ordered.map { it.id }, genreForMemory)
            Timber.d("Beat mix ${def.id}: ${ordered.size} songs, ${ordered.count { !it.known }} new, from ${candidates.size} candidates")

            return BeatMixContent(
                def = def,
                tracks = ordered.mapNotNull { candidate -> metadata[candidate.id]?.let { BeatMixTrack(it, !candidate.known) } },
                builtAt = System.currentTimeMillis(),
            )
        }

        /** Cover pictures already chosen for a mix, if any. */
        fun cachedCover(id: String): List<String>? = covers[id]

        /**
         * Up to three pictures for the cover of a mix: artist photos for artist mixes, album covers for the others.
         * They are drawn at random and renewed every day, with "Cambia i mix" and when the songs are renewed.
         */
        suspend fun cover(def: BeatMixDef): List<String> {
            covers[def.id]?.let { return it }
            val pictures = runCatching { withContext(Dispatchers.IO) { computeCover(def) } }.getOrDefault(emptyList())
            if (pictures.isNotEmpty()) covers[def.id] = pictures
            return pictures
        }

        private fun coverFromTracks(
            def: BeatMixDef,
            content: BeatMixContent,
        ): List<String> {
            val random = Random(System.nanoTime())
            val albums =
                content.tracks
                    .shuffled(random)
                    .distinctBy { it.metadata.artists.firstOrNull()?.name }
                    .mapNotNull { it.metadata.thumbnailUrl }
            val photos =
                if (def.kind in ArtistKinds) {
                    val taste = tasteCache
                    def.artists.mapNotNull { taste?.artistPhotos?.get(normalizeArtist(it)) }.shuffled(random) +
                        content.tracks.flatMap { it.metadata.artists }
                            .mapNotNull { taste?.artistPhotos?.get(normalizeArtist(it.name)) }
                            .shuffled(random)
                } else {
                    emptyList()
                }
            return (photos + albums).distinct().take(3).map { it.resize(544, 544) }
        }

        private suspend fun computeCover(def: BeatMixDef): List<String> {
            cached(def.id)?.let { content -> return coverFromTracks(def, content) }
            val taste = taste()
            val random = Random(System.nanoTime())

            fun albumsOf(songs: List<Song>) =
                songs.shuffled(random).distinctBy { it.artists.firstOrNull()?.name }.mapNotNull { it.song.thumbnailUrl }

            fun songsBy(artists: Set<String>) =
                taste.knownPool.filter { song -> song.artists.any { normalizeArtist(it.name) in artists } }

            val genre = BeatMixCatalog.genre(def.genreId)
            val pictures =
                when (def.kind) {
                    BeatMixKind.DAILY, BeatMixKind.ARTIST -> {
                        val artists = def.artists.map(::normalizeArtist)
                        artists.shuffled(random).mapNotNull { taste.artistPhotos[it] } + albumsOf(songsBy(artists.toSet()))
                    }
                    BeatMixKind.SIMILAR -> {
                        val artist = normalizeArtist(def.artists.firstOrNull().orEmpty())
                        listOfNotNull(taste.artistPhotos[artist]) + searchPictures(def.artists.firstOrNull().orEmpty())
                    }
                    BeatMixKind.GENRE, BeatMixKind.ORIGINAL, BeatMixKind.OUTSIDE -> {
                        val genreArtists = genre?.artists.orEmpty() + taste.profile.artistGenres.filterValues { genres -> genres.any { it == def.genreId } }.keys
                        val local = albumsOf(songsBy(genreArtists))
                        // Genres never listened to have no local pictures: one search gives typical covers.
                        if (local.size >= 3) local else local + searchPictures(genre?.seeds?.randomOrNull(random) ?: def.title)
                    }
                    BeatMixKind.DISCOVER -> albumsOf(taste.knownPool.take(120))
                    BeatMixKind.REDISCOVER -> albumsOf(taste.forgotten.ifEmpty { taste.knownPool })
                }
            return pictures.distinct().take(3).map { it.resize(544, 544) }
        }

        private suspend fun searchPictures(query: String): List<String> =
            if (query.isBlank()) emptyList() else coverSearches.withPermit { search(query, 6).map { it.thumbnail } }

        private sealed interface Seed {
            data class Query(val text: String) : Seed

            data class Track(val id: String) : Seed
        }

        /** YouTube Music radios started from each seed, fetched in parallel. */
        private suspend fun radios(seeds: List<Seed>): List<SongItem> =
            coroutineScope {
                seeds.map { seed ->
                    async {
                        val videoId =
                            when (seed) {
                                is Seed.Track -> seed.id
                                is Seed.Query -> search(seed.text, 1).firstOrNull()?.id
                            } ?: return@async emptyList()
                        val radio =
                            YouTube.next(WatchEndpoint(videoId = videoId, playlistId = "RDAMVM$videoId"))
                                .onFailure { Timber.w(it, "Beat radio failed for $videoId") }
                                .getOrNull()
                                ?.items
                                .orEmpty()
                        radio
                    }
                }.awaitAll().flatten()
            }

        private suspend fun search(
            query: String,
            limit: Int,
        ): List<SongItem> =
            YouTube
                .search(query, YouTube.SearchFilter.FILTER_SONG)
                .onFailure { Timber.w(it, "Beat search failed for $query") }
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                ?.take(limit)
                .orEmpty()

        /** Artists YouTube Music considers similar to the artist of [videoId]. */
        private suspend fun similarArtists(videoId: String): List<String> {
            val related = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()?.relatedEndpoint ?: return emptyList()
            return YouTube.related(related).getOrNull()?.artists?.map { it.title }.orEmpty()
        }

        private companion object {
            const val CONTENT_TTL_MS = 6L * 60 * 60 * 1000
            const val TASTE_TTL_MS = 20L * 60 * 1000
            val ArtistKinds = setOf(BeatMixKind.DAILY, BeatMixKind.ARTIST, BeatMixKind.SIMILAR)
            val GenreKinds = setOf(BeatMixKind.GENRE, BeatMixKind.ORIGINAL, BeatMixKind.OUTSIDE, BeatMixKind.DAILY)
        }
    }
