/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.FlingBehavior
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalTopBarHeightOffset
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.screens.search.OnlineSearchScreen
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The user's own YouTube playlist shown as the "Musica" card on the home screen. */
const val BEAT_MUSIC_PLAYLIST_TITLE = "Musica"

/** Id of that playlist on YouTube, so the card works even when the library list does not include it. */
const val BEAT_MUSIC_PLAYLIST_ID = "PLCOS7vGHf_Ps"

/** Where the "Musica" card stands: still loading, found (with the page to open) or not found anywhere. */
sealed interface BeatMusicPlaylist {
    data object Loading : BeatMusicPlaylist
    data object Missing : BeatMusicPlaylist
    data class Found(val route: String, val thumbnail: String?) : BeatMusicPlaylist
}

/**
 * Looks for the "Musica" playlist first among the YouTube account playlists, then among the playlists saved in the app,
 * so the card still works when the online list is slow or fails.
 */
fun findBeatMusicPlaylist(
    accountPlaylists: List<PlaylistItem>?,
    savedPlaylists: List<Playlist>?,
): BeatMusicPlaylist {
    fun matches(name: String?) = name?.trim()?.equals(BEAT_MUSIC_PLAYLIST_TITLE, ignoreCase = true) == true
    accountPlaylists?.firstOrNull { it.id.removePrefix("VL") == BEAT_MUSIC_PLAYLIST_ID }?.let {
        return BeatMusicPlaylist.Found("online_playlist/${it.id}", it.thumbnail)
    }
    accountPlaylists?.firstOrNull { matches(it.title) }?.let {
        return BeatMusicPlaylist.Found("online_playlist/${it.id}", it.thumbnail)
    }
    savedPlaylists?.firstOrNull { matches(it.playlist.name) }?.let { saved ->
        val browseId = saved.playlist.browseId
        return BeatMusicPlaylist.Found(
            if (browseId != null) "online_playlist/$browseId" else "local_playlist/${saved.id}",
            saved.playlist.thumbnailUrl ?: saved.thumbnails.firstOrNull(),
        )
    }
    // Not in the lists (or still loading): open the known playlist directly.
    return BeatMusicPlaylist.Found("online_playlist/$BEAT_MUSIC_PLAYLIST_ID", null)
}

/** Route of the page with everything that is not on the home ("Suggerimenti"). */
const val BEAT_SUGGESTIONS_ROUTE = "beat_suggestions"

/** Height of [BeatHomeSearchPill] including its vertical padding. */
val BeatSearchPillHeight = 68.dp

/**
 * Solid header holding [BeatHomeSearchPill]. It covers everything from the top of the screen down to the pill,
 * and the pill moves up together with the top app bar when the bar scrolls away.
 */
@Composable
fun BeatHomeSearchHeader(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBarOffset = LocalTopBarHeightOffset.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(beatBackgroundColor())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .layout { measurable, constraints ->
                val barSpace = (AppBarHeight.toPx() + topBarOffset()).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height + barSpace) { placeable.place(0, barSpace) }
            },
    ) {
        BeatHomeSearchPill(onClick = onClick)
    }
}

/** App background, honouring the pure black option. */
@Composable
fun beatBackgroundColor(): Color {
    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val systemDark = isSystemInDarkTheme()
    val pureBlack = pureBlackEnabled && (if (darkTheme == DarkMode.AUTO) systemDark else darkTheme == DarkMode.ON)
    return if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
}

/** Snaps a full-width horizontal grid one page (column) at a time. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberBeatPageFling(state: LazyGridState): FlingBehavior {
    val provider = remember(state) { SnapLayoutInfoProvider(lazyGridState = state, positionInLayout = { _, _ -> 0f }) }
    return rememberSnapFlingBehavior(provider)
}

/** Small dots under a full-width horizontal grid showing which page is on screen. */
@Composable
fun BeatPageDots(
    state: LazyGridState,
    itemCount: Int,
    rows: Int,
    modifier: Modifier = Modifier,
) {
    val pages = if (rows > 0) (itemCount + rows - 1) / rows else 0
    if (pages < 2) return
    val current by remember(state, rows, pages) {
        derivedStateOf {
            val width = state.layoutInfo.viewportSize.width
            val half = if (width > 0 && state.firstVisibleItemScrollOffset > width / 2) 1 else 0
            val atEnd = !state.canScrollForward
            if (atEnd) pages - 1 else (state.firstVisibleItemIndex / rows + half).coerceIn(0, pages - 1)
        }
    }
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        repeat(pages) { page ->
            val selected = page == current
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (selected) 16.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (selected) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f)),
            )
        }
    }
}

/** Tappable search field at the top of the home screen; opens [BeatHomeSearchOverlay]. */
@Composable
fun BeatHomeSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.beat_search_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Two big cards at the top of the home screen: liked songs and the "Musica" playlist. */
@Composable
fun BeatHomeShortcuts(
    musicPlaylist: BeatMusicPlaylist,
    onLikedClick: () -> Unit,
    onMusicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        BeatShortcutCard(
            title = stringResource(R.string.beat_liked),
            subtitle = stringResource(R.string.beat_liked_subtitle),
            iconRes = R.drawable.favorite,
            brush = Brush.linearGradient(listOf(colors.primary, colors.tertiary)),
            imageUrl = null,
            onClick = onLikedClick,
            modifier = Modifier.weight(1f),
        )
        BeatShortcutCard(
            title = BEAT_MUSIC_PLAYLIST_TITLE,
            subtitle = stringResource(
                when (musicPlaylist) {
                    is BeatMusicPlaylist.Found -> R.string.beat_music_subtitle
                    BeatMusicPlaylist.Loading -> R.string.beat_music_loading
                    BeatMusicPlaylist.Missing -> R.string.beat_music_missing
                },
            ),
            iconRes = R.drawable.queue_music,
            brush = Brush.linearGradient(listOf(colors.secondary, colors.primary)),
            imageUrl = (musicPlaylist as? BeatMusicPlaylist.Found)?.thumbnail,
            onClick = onMusicClick,
            modifier = Modifier
                .weight(1f)
                .alpha(if (musicPlaylist == BeatMusicPlaylist.Missing) 0.6f else 1f),
        )
    }
}

@Composable
private fun BeatShortcutCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    brush: Brush,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(brush)
            .clickable(onClick = onClick),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.35f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))),
        )
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(14.dp)
                .size(26.dp)
                .align(Alignment.TopStart),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Search shown in place of the home content, so search does not need its own tab.
 * Suggestions appear right under the field; submitting opens the usual results page.
 */
@Composable
fun BeatHomeSearchOverlay(onClose: () -> Unit) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val systemDark = isSystemInDarkTheme()
    val pureBlack = pureBlackEnabled && (if (darkTheme == DarkMode.AUTO) systemDark else darkTheme == DarkMode.ON)

    BackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isBlank()) return
        focusManager.clearFocus()
        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
            is YouTubeUrlParser.ParsedUrl.Video ->
                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = parsedUrl.id)))
            is YouTubeUrlParser.ParsedUrl.Playlist -> navController.navigate("online_playlist/${parsedUrl.id}")
            is YouTubeUrlParser.ParsedUrl.Album -> navController.navigate("album/MPREb_${parsedUrl.id}")
            is YouTubeUrlParser.ParsedUrl.Artist -> navController.navigate("artist/${parsedUrl.id}")
            null -> navController.navigate(SearchRoutes.resultRoute(searchQuery))
        }
        if (!pauseSearchHistory) {
            scope.launch(Dispatchers.IO) {
                database.query { insert(SearchHistory(query = searchQuery)) }
            }
        }
        // Close the overlay: coming back from the results shows the real home.
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { handleSearch(query.text) }),
                decorationBox = { inner ->
                    if (query.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.beat_search_hint),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
            if (query.text.isNotEmpty()) {
                IconButton(onClick = {
                    query = TextFieldValue()
                    runCatching { focusRequester.requestFocus() }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.clear),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)),
        ) {
            OnlineSearchScreen(
                query = query.text,
                onQueryChange = { query = it },
                onSearch = { handleSearch(it) },
                onDismiss = {},
                pureBlack = pureBlack,
            )
        }
    }
}

/** Big button at the bottom of the home that opens the suggestions page. */
@Composable
fun BeatSuggestionsButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.35f), colors.tertiary.copy(alpha = 0.25f))))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.explore_outlined),
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.beat_suggestions),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.beat_suggestions_subtitle),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
        )
    }
}

/** Title row of the suggestions page, with a back arrow. */
@Composable
fun BeatSuggestionsHeader(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = stringResource(R.string.dismiss),
            )
        }
        Text(
            text = stringResource(R.string.beat_suggestions),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
