/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * Beat genre mix: a YouTube Music radio started from a song matching [seedQuery]
 * (new music), with the user's liked songs by the same artists mixed in (known music).
 */
class BeatGenreMixQueue(
    private val title: String,
    private val seedQuery: String,
    private val likedSongs: List<Song>,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private var radio: YouTubeQueue? = null

    override suspend fun getInitialStatus(): Queue.Status =
        withContext(IO) {
            val found =
                YouTube
                    .search(seedQuery, YouTube.SearchFilter.FILTER_SONG)
                    .getOrThrow()
                    .items
                    .filterIsInstance<SongItem>()
            val seed = found.firstOrNull() ?: error("No song found for $seedQuery")

            val radioQueue = YouTubeQueue(WatchEndpoint(videoId = seed.id, playlistId = "RDAMVM${seed.id}"))
            radio = radioQueue
            val newItems = radioQueue.getInitialStatus().items.distinctBy { it.mediaId }

            val artistNames =
                (newItems.flatMap { item -> item.metadata?.artists.orEmpty().map { it.name } } +
                    found.flatMap { song -> song.artists.map { it.name } })
                    .map { it.lowercase().trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            val newIds = newItems.map { it.mediaId }.toSet()
            val knownItems =
                likedSongs
                    .filter { song -> song.id !in newIds && song.artists.any { it.name.lowercase().trim() in artistNames } }
                    .shuffled()
                    .take(MAX_KNOWN_SONGS)
                    .map { it.toMediaItem() }

            Queue.Status(title = title, items = interleave(newItems, knownItems), mediaItemIndex = 0)
        }

    override fun hasNextPage(): Boolean = radio?.hasNextPage() == true

    override suspend fun nextPage(): List<MediaItem> = radio?.nextPage().orEmpty()

    // One known song every few new ones, starting with the radio seed.
    private fun interleave(
        newItems: List<MediaItem>,
        knownItems: List<MediaItem>,
    ): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val known = knownItems.iterator()
        newItems.forEachIndexed { index, item ->
            result.add(item)
            if (index % 2 == 1 && known.hasNext()) result.add(known.next())
        }
        known.forEach { result.add(it) }
        return result
    }

    private companion object {
        const val MAX_KNOWN_SONGS = 15
    }
}
