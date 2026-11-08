/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.playback.queues.BeatGenreMixQueue

/** A genre mix: each start picks one of [seeds] at random, so the mix changes every time. */
private data class BeatGenreMix(
    val title: String,
    val seeds: List<String>,
    val colors: List<Color>,
)

private val GenreMixes =
    listOf(
        BeatGenreMix(
            "Acid jazz",
            listOf("Jamiroquai Virtual Insanity", "Jamiroquai Cosmic Girl", "The Brand New Heavies Never Stop", "Incognito Still a Friend of Mine"),
            listOf(Color(0xFFFF8A00), Color(0xFF8E2DE2)),
        ),
        BeatGenreMix(
            "Anni '80",
            listOf("a-ha Take On Me", "Wham Wake Me Up Before You Go-Go", "Duran Duran Rio", "Cyndi Lauper Girls Just Want to Have Fun"),
            listOf(Color(0xFFFF2E93), Color(0xFF00D4FF)),
        ),
        BeatGenreMix(
            "Anni '80 action e rock",
            listOf("Survivor Eye of the Tiger", "Europe The Final Countdown", "Bon Jovi Livin on a Prayer", "Kenny Loggins Danger Zone"),
            listOf(Color(0xFFE53935), Color(0xFF212121)),
        ),
        BeatGenreMix(
            "Anni '70",
            listOf("Bee Gees Stayin Alive", "ABBA Dancing Queen", "Earth Wind and Fire September", "Boney M Rasputin"),
            listOf(Color(0xFFF9A825), Color(0xFFD84315)),
        ),
        BeatGenreMix(
            "Anni 2000",
            listOf("Outkast Hey Ya", "Black Eyed Peas I Gotta Feeling", "Shakira Hips Dont Lie", "Eminem Without Me"),
            listOf(Color(0xFF00C853), Color(0xFF1565C0)),
        ),
        BeatGenreMix(
            "House e disco",
            listOf("Daft Punk One More Time", "Stardust Music Sounds Better With You", "Modjo Lady Hear Me Tonight", "Purple Disco Machine Hypnotized"),
            listOf(Color(0xFF7C4DFF), Color(0xFFFF4081)),
        ),
    )

private val ArtistMixColors =
    listOf(
        listOf(Color(0xFF1DB954), Color(0xFF0D47A1)),
        listOf(Color(0xFFFF6F00), Color(0xFFC2185B)),
        listOf(Color(0xFF00BFA5), Color(0xFF4A148C)),
    )

/**
 * Home row of genre mixes plus mixes for the artists liked the most.
 * Each mix blends liked songs with new ones from YouTube Music.
 */
@Composable
fun BeatGenreMixRow(modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val likedSongs by remember { database.likedSongsByNameAsc() }.collectAsStateWithLifecycle(initialValue = emptyList())

    val artistMixes =
        remember(likedSongs) {
            likedSongs
                .flatMap { song -> song.artists.map { it.name } }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(ArtistMixColors.size)
                .mapIndexed { index, (name, _) -> BeatGenreMix("Mix $name", listOf(name), ArtistMixColors[index]) }
        }
    val mixes = GenreMixes + artistMixes
    val subtitle = stringResource(R.string.beat_genre_mix_subtitle)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.beat_genre_mixes),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(mixes, key = { it.title }) { mix ->
                BeatGenreMixCard(
                    title = mix.title,
                    subtitle = subtitle,
                    colors = mix.colors,
                    onClick = {
                        playerConnection.playQueue(
                            BeatGenreMixQueue(
                                title = mix.title,
                                seedQuery = mix.seeds.random(),
                                likedSongs = likedSongs,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun BeatGenreMixCard(
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(colors))
                .clickable(onClick = onClick)
                .padding(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.play),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).size(22.dp),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}
