/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.beat

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** What a mix is built from; decides the sources and how much new music it contains. */
enum class BeatMixKind(val newShare: Double) {
    /** "Daily Mix": a group of the artists listened to the most, plus similar ones. */
    DAILY(0.45),

    /** A genre the user listens to. */
    GENRE(0.55),

    /** One of the user's original mixes. */
    ORIGINAL(0.5),

    /** One artist and the music around it. */
    ARTIST(0.4),

    /** Artists similar to one the user loves, without that artist. */
    SIMILAR(0.85),

    /** Songs loved in the past and not heard for a while. */
    REDISCOVER(0.25),

    /** Almost only songs never heard, close to the user's taste. */
    DISCOVER(0.95),

    /** A compatible genre the user does not listen to yet. */
    OUTSIDE(0.85),
}

/** A mix as shown in the lists: everything needed to build its songs later. */
data class BeatMixDef(
    val id: String,
    val kind: BeatMixKind,
    val title: String,
    val subtitle: String,
    val colors: Pair<Long, Long>,
    val genreId: String? = null,
    /** Display names of the artists the mix is about. */
    val artists: List<String> = emptyList(),
)

/** Listening time of one artist, in milliseconds. */
data class BeatArtistListen(
    val name: String,
    val recentMs: Long,
    val totalMs: Long,
)

/** What Beat knows about the user's taste, rebuilt from the listening history so it keeps learning. */
class BeatTasteProfile(
    /** Normalized artist name to score (higher = loved more). */
    val artistScores: Map<String, Double>,
    /** Normalized artist name to display name. */
    val artistNames: Map<String, String>,
    /** Genre id to affinity between 0 and 1. */
    val genreAffinity: Map<String, Double>,
    /** Normalized artist name to the genres it belongs to (catalog plus what was learned). */
    val artistGenres: Map<String, List<String>>,
) {
    val isEmpty: Boolean get() = artistScores.isEmpty()

    fun topArtists(limit: Int): List<String> =
        artistScores.entries.sortedByDescending { it.value }.take(limit).map { it.key }

    fun displayName(normalized: String): String = artistNames[normalized] ?: normalized

    companion object {
        /**
         * @param likedByArtist number of liked songs per normalized artist name.
         * @param skipsByArtist recent skips per normalized artist name.
         * @param learnedGenres normalized artist name to votes per genre, collected when songs coming from a
         *        genre mix were listened to until the end.
         */
        fun build(
            listens: List<BeatArtistListen>,
            likedByArtist: Map<String, Int>,
            skipsByArtist: Map<String, Int>,
            learnedGenres: Map<String, Map<String, Int>>,
            displayNames: Map<String, String> = emptyMap(),
        ): BeatTasteProfile {
            val scores = HashMap<String, Double>()
            val names = HashMap<String, String>(displayNames)
            listens.forEach { listen ->
                val key = normalizeArtist(listen.name)
                if (key.isEmpty()) return@forEach
                names.putIfAbsent(key, listen.name)
                val recentMin = listen.recentMs / 60_000.0
                val olderMin = max(0L, listen.totalMs - listen.recentMs) / 60_000.0
                scores[key] = (scores[key] ?: 0.0) + recentMin + 0.35 * olderMin
            }
            likedByArtist.forEach { (artist, count) ->
                if (artist.isEmpty()) return@forEach
                scores[artist] = (scores[artist] ?: 0.0) + 4.0 * count
            }
            skipsByArtist.forEach { (artist, count) ->
                val current = scores[artist] ?: return@forEach
                scores[artist] = max(0.0, current - 2.0 * count)
            }
            scores.entries.removeAll { it.value <= 0.0 }

            val artistGenres = HashMap<String, List<String>>()
            val affinity = HashMap<String, Double>()
            scores.forEach { (artist, score) ->
                val genres = genresFor(artist, learnedGenres)
                if (genres.isEmpty()) return@forEach
                artistGenres[artist] = genres
                // An artist that belongs to several genres splits its weight between them.
                genres.forEach { affinity[it] = (affinity[it] ?: 0.0) + score / genres.size }
            }
            val maxAffinity = affinity.values.maxOrNull() ?: 0.0
            val normalized =
                if (maxAffinity > 0) affinity.mapValues { it.value / maxAffinity } else emptyMap()
            return BeatTasteProfile(scores, names, normalized, artistGenres)
        }

        fun genresFor(
            artist: String,
            learnedGenres: Map<String, Map<String, Int>>,
        ): List<String> {
            val known = BeatMixCatalog.genresOfArtist(artist)
            val learned =
                learnedGenres[artist].orEmpty()
                    .filter { it.value >= 2 && BeatMixCatalog.genre(it.key) != null }
                    .entries.sortedByDescending { it.value }
                    .take(2)
                    .map { it.key }
            return (known + learned).distinct()
        }
    }
}

/** The mixes proposed today, grouped as on the screen. */
data class BeatMixLineup(
    val day: Long,
    val rotation: Int,
    val hero: BeatMixDef?,
    val forYou: List<BeatMixDef>,
    val discover: List<BeatMixDef>,
    val originals: List<BeatMixDef>,
    val artistMixes: List<BeatMixDef>,
    val genres: List<BeatMixDef>,
    val moreGenres: List<BeatMixDef>,
) {
    val all: List<BeatMixDef>
        get() = (listOfNotNull(hero) + forYou + discover + originals + artistMixes + genres + moreGenres).distinctBy { it.id }

    fun find(id: String): BeatMixDef? = all.firstOrNull { it.id == id }
}

/**
 * Chooses which mixes to show. The choice depends on the day and on [rotation] (the "change mixes" button),
 * so the home changes every day and on request, while staying the same if the app is reopened.
 */
object BeatMixPlanner {
    private val DailyColors =
        listOf(
            0xFF1DB954 to 0xFF0D47A1,
            0xFFFF6F00 to 0xFFC2185B,
            0xFF00BFA5 to 0xFF4A148C,
            0xFF2962FF to 0xFF00E5FF,
            0xFFAA00FF to 0xFFFF6D00,
            0xFFFFD600 to 0xFFD50000,
        )

    fun plan(
        profile: BeatTasteProfile,
        day: Long,
        rotation: Int,
        hasRediscover: Boolean,
    ): BeatMixLineup {
        val random = Random(day * 7919 + rotation * 104_729L)

        val originals =
            BeatMixCatalog.originals.map { genre ->
                BeatMixDef(
                    id = "original:${genre.id}",
                    kind = BeatMixKind.ORIGINAL,
                    title = genre.name,
                    subtitle = "Il tuo mix",
                    colors = genre.colors,
                    genreId = genre.id,
                )
            }

        val daily = dailyMixes(profile, random)

        val rankedGenres =
            BeatMixCatalog.genres
                .filter { (profile.genreAffinity[it.id] ?: 0.0) > 0.03 }
                .sortedByDescending { profile.genreAffinity[it.id] ?: 0.0 }
        // Genres the user listens to, picked with a weighted draw so they rotate. The original genres already
        // have their own row ("I miei mix"), so here they leave room for the others.
        val yourGenres =
            weightedSample(rankedGenres.filter { !it.original }.take(8), 4, random) { (profile.genreAffinity[it.id] ?: 0.0) + 0.15 }
                .map { genreMix(it, BeatMixKind.GENRE, "Dai tuoi ascolti, con brani nuovi") }

        val discover = mutableListOf<BeatMixDef>()
        discover +=
            BeatMixDef(
                id = "discover",
                kind = BeatMixKind.DISCOVER,
                title = "Radar novità",
                subtitle = "Brani mai ascoltati, vicini ai tuoi gusti",
                colors = 0xFF00E5FF to 0xFF6200EA,
            )
        val similarSeeds = weightedSample(profile.topArtists(12), 2, random) { profile.artistScores[it] ?: 0.0 }
        similarSeeds.forEachIndexed { index, artist ->
            val name = profile.displayName(artist)
            discover +=
                BeatMixDef(
                    id = "similar:$artist",
                    kind = BeatMixKind.SIMILAR,
                    title = "Simili a $name",
                    subtitle = "Artisti vicini a $name",
                    colors = DailyColors[(index + 3) % DailyColors.size],
                    artists = listOf(name),
                )
        }
        outsideGenres(profile, rankedGenres)
            .let { candidates -> weightedSample(candidates, 2, random) { it.second } }
            .forEach { (genre, _) ->
                discover += genreMix(genre, BeatMixKind.OUTSIDE, "Fuori dalle tue abitudini")
            }
        if (hasRediscover) {
            discover +=
                BeatMixDef(
                    id = "rediscover",
                    kind = BeatMixKind.REDISCOVER,
                    title = "Riscoperte",
                    subtitle = "Brani che amavi e non ascolti da un po'",
                    colors = 0xFFFFB300 to 0xFF5D4037,
                )
        }

        val artistMixes =
            profile.topArtists(8).mapIndexed { index, artist ->
                val name = profile.displayName(artist)
                BeatMixDef(
                    id = "artist:$artist",
                    kind = BeatMixKind.ARTIST,
                    title = "Mix $name",
                    subtitle = "$name e artisti simili",
                    colors = DailyColors[index % DailyColors.size],
                    artists = listOf(name),
                )
            }

        val genreMixes = rankedGenres.map { genreMix(it, BeatMixKind.GENRE, "Dai tuoi ascolti, con brani nuovi") }
        val usedGenres = rankedGenres.map { it.id }.toSet()
        val moreGenres =
            BeatMixCatalog.genres
                .filter { it.id !in usedGenres && !it.original }
                .map { genreMix(it, BeatMixKind.OUTSIDE, "Da esplorare") }

        val forYou = (daily + yourGenres).distinctBy { it.id }
        val heroPool = forYou + discover.filter { it.kind != BeatMixKind.REDISCOVER } + originals
        val hero = heroPool.getOrNull(random.nextInt(heroPool.size))

        return BeatMixLineup(
            day = day,
            rotation = rotation,
            hero = hero,
            forYou = forYou.filter { it.id != hero?.id }.ifEmpty { forYou },
            discover = discover.filter { it.id != hero?.id },
            originals = originals,
            artistMixes = artistMixes,
            genres = genreMixes,
            moreGenres = moreGenres,
        )
    }

    /**
     * Rebuilds a mix from its id when it is not in today's selection any more
     * (for example a mix opened yesterday and reopened after "Cambia i mix").
     */
    fun fromId(
        id: String,
        profile: BeatTasteProfile,
    ): BeatMixDef? {
        val kind = id.substringBefore(':')
        val value = id.substringAfter(':', "")
        val genre = BeatMixCatalog.genre(value)
        return when (kind) {
            "original" ->
                genre?.let {
                    BeatMixDef(id, BeatMixKind.ORIGINAL, it.name, "Il tuo mix", it.colors, genreId = it.id)
                }
            "genre" -> genre?.let { genreMix(it, BeatMixKind.GENRE, "Dai tuoi ascolti, con brani nuovi") }
            "outside" -> genre?.let { genreMix(it, BeatMixKind.OUTSIDE, "Fuori dalle tue abitudini") }
            "artist", "similar" -> {
                if (value.isEmpty()) return null
                val name = profile.displayName(value)
                if (kind == "artist") {
                    BeatMixDef(id, BeatMixKind.ARTIST, "Mix $name", "$name e artisti simili", DailyColors[0], artists = listOf(name))
                } else {
                    BeatMixDef(id, BeatMixKind.SIMILAR, "Simili a $name", "Artisti vicini a $name", DailyColors[3], artists = listOf(name))
                }
            }
            "discover" -> BeatMixDef(id, BeatMixKind.DISCOVER, "Radar novità", "Brani mai ascoltati, vicini ai tuoi gusti", 0xFF00E5FF to 0xFF6200EA)
            "rediscover" ->
                BeatMixDef(id, BeatMixKind.REDISCOVER, "Riscoperte", "Brani che amavi e non ascolti da un po'", 0xFFFFB300 to 0xFF5D4037)
            else -> null
        }
    }

    private fun genreMix(
        genre: BeatGenre,
        kind: BeatMixKind,
        subtitle: String,
    ) = BeatMixDef(
        id = "${kind.name.lowercase()}:${genre.id}",
        kind = kind,
        title = genre.name,
        subtitle = subtitle,
        colors = genre.colors,
        genreId = genre.id,
    )

    /** Compatible genres with little listening: the "outside your habits" proposals. */
    private fun outsideGenres(
        profile: BeatTasteProfile,
        rankedGenres: List<BeatGenre>,
    ): List<Pair<BeatGenre, Double>> {
        val weights = HashMap<String, Double>()
        rankedGenres.take(6).forEach { genre ->
            val parent = profile.genreAffinity[genre.id] ?: 0.0
            genre.neighbors.forEach { neighbor ->
                if ((profile.genreAffinity[neighbor] ?: 0.0) < 0.15) {
                    weights[neighbor] = (weights[neighbor] ?: 0.0) + parent
                }
            }
        }
        if (weights.isEmpty()) {
            // Nothing listened yet: propose the user's original genres' neighbours.
            BeatMixCatalog.originals.flatMap { it.neighbors }.forEach { weights[it] = (weights[it] ?: 0.0) + 1.0 }
        }
        return weights.mapNotNull { (id, weight) -> BeatMixCatalog.genre(id)?.let { it to weight } }
    }

    /**
     * Up to four "Daily Mix": a weighted draw among the top artists (so the selection rotates),
     * grouped by their main genre so each mix has a coherent sound.
     */
    private fun dailyMixes(
        profile: BeatTasteProfile,
        random: Random,
    ): List<BeatMixDef> {
        if (profile.isEmpty) return emptyList()
        val pool = profile.topArtists(30)
        val picked = weightedSample(pool, min(16, pool.size), random) { profile.artistScores[it] ?: 0.0 }

        val groups = LinkedHashMap<String, MutableList<String>>()
        val unknown = mutableListOf<String>()
        picked.forEach { artist ->
            val genre =
                profile.artistGenres[artist].orEmpty().maxByOrNull { profile.genreAffinity[it] ?: 0.0 }
            if (genre == null) unknown += artist else groups.getOrPut(genre) { mutableListOf() } += artist
        }
        val clusters =
            groups.entries
                .sortedByDescending { entry -> entry.value.sumOf { profile.artistScores[it] ?: 0.0 } }
                .take(4)
                .map { it.key to it.value.toMutableList() }
                .toMutableList()
        // Artists without a known genre, or from smaller groups, join the smallest mixes.
        val leftovers =
            unknown + groups.filterKeys { key -> clusters.none { it.first == key } }.values.flatten()
        leftovers.forEach { artist ->
            if (clusters.size < 4 && (clusters.isEmpty() || clusters.all { it.second.size >= 3 })) {
                clusters += ("" to mutableListOf(artist))
            } else {
                clusters.minByOrNull { it.second.size }?.second?.add(artist)
            }
        }

        return clusters.filter { it.second.isNotEmpty() }.mapIndexed { index, (genreId, artists) ->
            val names = artists.map { profile.displayName(it) }
            BeatMixDef(
                id = "daily:${index + 1}:${artists.first()}",
                kind = BeatMixKind.DAILY,
                title = "Daily Mix ${index + 1}",
                subtitle = names.take(3).joinToString(", ") + if (names.size > 3) " e altri" else "",
                colors = BeatMixCatalog.genre(genreId)?.colors ?: DailyColors[index % DailyColors.size],
                genreId = genreId.ifEmpty { null },
                artists = names,
            )
        }
    }
}

/** A song that can enter a mix. */
data class BeatCandidate(
    val id: String,
    /** Normalized artist names. */
    val artists: List<String>,
    /** Already listened to (or liked) by the user. */
    val known: Boolean,
    /** Relevance for this mix, before rotation penalties. */
    val relevance: Double = 1.0,
)

/** Signals that keep the mixes changing and learning. */
data class BeatRotationState(
    /** Song id to days since it was last proposed in a mix. */
    val servedDaysAgo: Map<String, Double> = emptyMap(),
    /** Songs heard a lot in the last two weeks. */
    val heavyRotation: Set<String> = emptySet(),
    /** Song id to recent skips. */
    val songSkips: Map<String, Int> = emptyMap(),
    /** Normalized artist name to recent skips. */
    val artistSkips: Map<String, Int> = emptyMap(),
    val disliked: Set<String> = emptySet(),
)

object BeatMixSelector {
    const val DEFAULT_SIZE = 50
    private const val MAX_PER_ARTIST = 4

    /**
     * Picks the songs of a mix: a weighted random draw that keeps the share of new songs requested by the mix,
     * avoids songs proposed in the last days, songs skipped often and disliked ones, and limits songs per artist.
     */
    fun select(
        candidates: List<BeatCandidate>,
        newShare: Double,
        state: BeatRotationState,
        random: Random,
        size: Int = DEFAULT_SIZE,
        maxPerArtist: Int = MAX_PER_ARTIST,
    ): List<BeatCandidate> {
        val usable =
            candidates
                .distinctBy { it.id }
                .filter { it.id !in state.disliked && (state.songSkips[it.id] ?: 0) < 2 }
        val (fresh, known) = usable.partition { !it.known }
        val wantedNew = (size * newShare).toInt().coerceAtMost(fresh.size)
        val wantedKnown = (size - wantedNew).coerceAtMost(known.size)

        val perArtist = HashMap<String, Int>()
        fun draw(
            pool: List<BeatCandidate>,
            count: Int,
        ): List<BeatCandidate> {
            val ordered = weightedOrder(pool, random) { weight(it, state) }
            val chosen = mutableListOf<BeatCandidate>()
            for (candidate in ordered) {
                if (chosen.size >= count) break
                val main = candidate.artists.firstOrNull().orEmpty()
                if ((perArtist[main] ?: 0) >= maxPerArtist) continue
                perArtist[main] = (perArtist[main] ?: 0) + 1
                chosen += candidate
            }
            return chosen
        }

        val chosenNew = draw(fresh, wantedNew)
        val chosenKnown = draw(known, wantedKnown)
        // If one side ran short, fill up with the other so the mix keeps its length.
        val missing = size - chosenNew.size - chosenKnown.size
        val filler =
            if (missing > 0) {
                val taken = (chosenNew + chosenKnown).map { it.id }.toSet()
                draw(usable.filter { it.id !in taken }, missing)
            } else {
                emptyList()
            }
        return chosenNew + chosenKnown + filler
    }

    fun weight(
        candidate: BeatCandidate,
        state: BeatRotationState,
    ): Double {
        var weight = max(candidate.relevance, 0.01)
        state.servedDaysAgo[candidate.id]?.let { days ->
            weight *=
                when {
                    days < 1 -> 0.08
                    days < 3 -> 0.2
                    days < 7 -> 0.45
                    days < 14 -> 0.75
                    else -> 1.0
                }
        }
        if (candidate.id in state.heavyRotation) weight *= 0.3
        val artistSkips = candidate.artists.sumOf { state.artistSkips[it] ?: 0 }
        if (artistSkips > 0) weight /= 1.0 + 0.35 * artistSkips
        if ((state.songSkips[candidate.id] ?: 0) == 1) weight *= 0.4
        return weight
    }

    /** Efraimidis–Spirakis weighted random order: heavier items tend to come first, but every run differs. */
    fun <T> weightedOrder(
        items: List<T>,
        random: Random,
        weight: (T) -> Double,
    ): List<T> =
        items
            .map { item ->
                val w = max(weight(item), 1e-6)
                val u = random.nextDouble().coerceIn(1e-12, 1.0)
                item to ln(u) / w
            }.sortedByDescending { it.second }
            .map { it.first }
}

/**
 * Shuffle that sounds random to a person: songs by the same artist are kept apart
 * and new and known songs alternate evenly instead of coming in blocks.
 */
object BeatSmartShuffle {
    fun shuffle(
        items: List<BeatCandidate>,
        random: Random,
        minArtistGap: Int = 3,
    ): List<BeatCandidate> {
        if (items.size < 3) return items.shuffled(random)
        val fresh = items.filter { !it.known }.shuffled(random).toMutableList()
        val known = items.filter { it.known }.shuffled(random).toMutableList()
        val result = ArrayList<BeatCandidate>(items.size)
        val total = items.size.toDouble()
        val freshShare = fresh.size / total

        while (fresh.isNotEmpty() || known.isNotEmpty()) {
            val placedFresh = result.count { !it.known }
            val wantFresh =
                when {
                    known.isEmpty() -> true
                    fresh.isEmpty() -> false
                    else -> placedFresh < (result.size + 1) * freshShare
                }
            val primary = if (wantFresh) fresh else known
            val secondary = if (wantFresh) known else fresh
            val pick = takeSpaced(primary, result, minArtistGap) ?: takeSpaced(secondary, result, minArtistGap)
            result += pick ?: (primary.ifEmpty { secondary }).removeAt(0)
        }
        return result
    }

    private fun takeSpaced(
        pool: MutableList<BeatCandidate>,
        placed: List<BeatCandidate>,
        gap: Int,
    ): BeatCandidate? {
        val recentArtists = placed.takeLast(gap).flatMap { it.artists }.toSet()
        val index = pool.indexOfFirst { candidate -> candidate.artists.none { it in recentArtists } }
        return if (index >= 0) pool.removeAt(index) else null
    }
}

/** Weighted draw without replacement of up to [count] items. */
fun <T> weightedSample(
    items: List<T>,
    count: Int,
    random: Random,
    weight: (T) -> Double,
): List<T> = BeatMixSelector.weightedOrder(items, random, weight).take(count)

/** Days since the epoch in the local time zone, used to change the home every day. */
fun beatDayNumber(nowMs: Long = System.currentTimeMillis()): Long {
    val offset = java.util.TimeZone.getDefault().getOffset(nowMs)
    return (nowMs + offset) / 86_400_000L
}
