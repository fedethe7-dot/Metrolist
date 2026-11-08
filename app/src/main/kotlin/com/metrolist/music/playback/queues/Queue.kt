/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) filterItems { it.metadata?.explicit != true } else this

        fun filterVideoSongs(disableVideos: Boolean = false) =
            if (disableVideos) filterItems { it.metadata?.isVideoSong != true } else this

        // The song the user picked stays, even if disliked: only the others are dropped.
        fun filterDisliked(dislikedIds: Set<String>): Status {
            if (dislikedIds.isEmpty()) return this
            val startId = items.getOrNull(mediaItemIndex)?.mediaId
            return filterItems { it.mediaId == startId || it.mediaId !in dislikedIds }
        }

        // Keeps mediaItemIndex on the same song; if that song is removed, starts at the next kept one.
        private fun filterItems(keep: (MediaItem) -> Boolean): Status {
            val filtered = items.filter(keep)
            if (filtered.size == items.size) return this
            val start = items.getOrNull(mediaItemIndex)
            val keptBefore = items.take(mediaItemIndex.coerceAtLeast(0)).count(keep)
            return copy(
                items = filtered,
                mediaItemIndex = keptBefore.coerceAtMost((filtered.size - 1).coerceAtLeast(0)),
                position = if (start != null && keep(start)) position else 0L,
            )
        }
    }
}

fun List<MediaItem>.filterDisliked(dislikedIds: Set<String>) =
    if (dislikedIds.isEmpty()) this else filterNot { it.mediaId in dislikedIds }

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.metadata?.isVideoSong == true }
    } else {
        this
    }
