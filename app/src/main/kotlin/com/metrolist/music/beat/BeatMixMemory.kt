/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.beat

import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Small on-device memory of the mix engine: which songs were proposed and when (so mixes rotate),
 * which songs and artists get skipped, and which genre the songs listened from a genre mix belong to
 * (so Beat learns the genres of new artists). Stored as JSON in the app's private files.
 */
class BeatMixMemory(
    private val file: File,
) {
    private val served = LinkedHashMap<String, Long>()
    private val origin = LinkedHashMap<String, String>()
    private val skips = ArrayList<Skip>()
    private val genreVotes = HashMap<String, HashMap<String, Int>>()
    private var rotation = 0
    private var loaded = false

    private data class Skip(
        val id: String,
        val artists: List<String>,
        val time: Long,
    )

    @Synchronized
    fun rotation(): Int {
        ensureLoaded()
        return rotation
    }

    @Synchronized
    fun nextRotation(): Int {
        ensureLoaded()
        rotation++
        save()
        return rotation
    }

    /** Remembers that these songs were proposed in a mix, and for genre mixes which genre they came from. */
    @Synchronized
    fun recordServed(
        ids: Collection<String>,
        genreId: String?,
        now: Long = System.currentTimeMillis(),
    ) {
        ensureLoaded()
        ids.forEach { id ->
            served.remove(id)
            served[id] = now
            if (genreId != null) {
                origin.remove(id)
                origin[id] = genreId
            }
        }
        trim(now)
        save()
    }

    @Synchronized
    fun recordSkip(
        id: String,
        artists: List<String>,
        now: Long = System.currentTimeMillis(),
    ) {
        ensureLoaded()
        skips += Skip(id, artists.map(::normalizeArtist), now)
        trim(now)
        save()
    }

    /** A song coming from a genre mix was listened to: its artists vote for that genre. */
    @Synchronized
    fun recordListened(
        id: String,
        artists: List<String>,
    ) {
        ensureLoaded()
        val genre = origin[id] ?: return
        artists.map(::normalizeArtist).filter { it.isNotEmpty() }.forEach { artist ->
            val votes = genreVotes.getOrPut(artist) { HashMap() }
            votes[genre] = (votes[genre] ?: 0) + 1
        }
        save()
    }

    @Synchronized
    fun rotationState(
        heavyRotation: Set<String>,
        disliked: Set<String>,
        now: Long = System.currentTimeMillis(),
    ): BeatRotationState {
        ensureLoaded()
        val recentSkips = skips.filter { now - it.time < SKIP_MEMORY_MS }
        return BeatRotationState(
            servedDaysAgo = served.mapValues { (now - it.value) / 86_400_000.0 },
            heavyRotation = heavyRotation,
            songSkips = recentSkips.groupingBy { it.id }.eachCount(),
            artistSkips = artistSkips(recentSkips),
            disliked = disliked,
        )
    }

    @Synchronized
    fun artistSkips(now: Long = System.currentTimeMillis()): Map<String, Int> {
        ensureLoaded()
        return artistSkips(skips.filter { now - it.time < SKIP_MEMORY_MS })
    }

    @Synchronized
    fun learnedGenres(): Map<String, Map<String, Int>> {
        ensureLoaded()
        return genreVotes.mapValues { HashMap(it.value) }
    }

    private fun artistSkips(list: List<Skip>): Map<String, Int> {
        val result = HashMap<String, Int>()
        // Only the main artist is blamed for a skip.
        list.forEach { skip -> skip.artists.firstOrNull()?.let { result[it] = (result[it] ?: 0) + 1 } }
        return result
    }

    private fun trim(now: Long) {
        served.entries.removeAll { now - it.value > SERVED_MEMORY_MS }
        while (served.size > MAX_SERVED) served.remove(served.keys.first())
        while (origin.size > MAX_ORIGIN) origin.remove(origin.keys.first())
        skips.removeAll { now - it.time > SKIP_MEMORY_MS }
        while (skips.size > MAX_SKIPS) skips.removeAt(0)
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        if (!file.exists()) return
        runCatching {
            val json = JSONObject(file.readText())
            rotation = json.optInt("rotation", 0)
            json.optJSONObject("served")?.let { obj -> obj.keys().forEach { served[it] = obj.getLong(it) } }
            json.optJSONObject("origin")?.let { obj -> obj.keys().forEach { origin[it] = obj.getString(it) } }
            json.optJSONArray("skips")?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val artists = item.optJSONArray("a")
                    skips +=
                        Skip(
                            id = item.getString("id"),
                            artists = List(artists?.length() ?: 0) { artists!!.getString(it) },
                            time = item.getLong("t"),
                        )
                }
            }
            json.optJSONObject("votes")?.let { obj ->
                obj.keys().forEach { artist ->
                    val votes = obj.getJSONObject(artist)
                    genreVotes[artist] = HashMap<String, Int>().apply { votes.keys().forEach { put(it, votes.getInt(it)) } }
                }
            }
        }.onFailure { Timber.w(it, "Beat mix memory unreadable, starting fresh") }
    }

    private fun save() {
        runCatching {
            val json =
                JSONObject().apply {
                    put("rotation", rotation)
                    put("served", JSONObject().apply { served.forEach { (k, v) -> put(k, v) } })
                    put("origin", JSONObject().apply { origin.forEach { (k, v) -> put(k, v) } })
                    put(
                        "skips",
                        JSONArray().apply {
                            skips.forEach { skip ->
                                put(JSONObject().put("id", skip.id).put("a", JSONArray(skip.artists)).put("t", skip.time))
                            }
                        },
                    )
                    put(
                        "votes",
                        JSONObject().apply {
                            genreVotes.forEach { (artist, votes) -> put(artist, JSONObject(votes as Map<*, *>)) }
                        },
                    )
                }
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.toString())
            if (!tmp.renameTo(file)) {
                file.writeText(json.toString())
                tmp.delete()
            }
        }.onFailure { Timber.w(it, "Could not save Beat mix memory") }
    }

    private companion object {
        const val SERVED_MEMORY_MS = 30L * 86_400_000L
        const val SKIP_MEMORY_MS = 60L * 86_400_000L
        const val MAX_SERVED = 5000
        const val MAX_ORIGIN = 3000
        const val MAX_SKIPS = 1500
    }
}
