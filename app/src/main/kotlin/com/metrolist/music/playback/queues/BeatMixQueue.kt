/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.models.MediaMetadata

/**
 * A Beat mix in the player: its songs in the chosen order, starting from [startIndex].
 * When the mix ends the music goes on with a YouTube Music radio started from [continueFromId],
 * skipping songs that were already in the mix.
 */
class BeatMixQueue(
    private val title: String,
    private val items: List<MediaItem>,
    private val startIndex: Int = 0,
    private val continueFromId: String? = null,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private var radio: YouTubeQueue? = null
    private var radioStarted = false
    private val played = items.map { it.mediaId }.toHashSet()

    override suspend fun getInitialStatus(): Queue.Status =
        Queue.Status(title = title, items = items, mediaItemIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))

    override fun hasNextPage(): Boolean = if (!radioStarted) continueFromId != null else radio?.hasNextPage() == true

    override suspend fun nextPage(): List<MediaItem> {
        val next =
            if (!radioStarted) {
                radioStarted = true
                val id = continueFromId ?: return emptyList()
                val queue = YouTubeQueue(WatchEndpoint(videoId = id, playlistId = "RDAMVM$id"))
                radio = queue
                runCatching { queue.getInitialStatus().items }.getOrDefault(emptyList())
            } else {
                runCatching { radio?.nextPage().orEmpty() }.getOrDefault(emptyList())
            }
        return next.filter { played.add(it.mediaId) }
    }
}
