/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.beat.BeatCandidate
import com.metrolist.music.beat.BeatMixContent
import com.metrolist.music.beat.BeatMixDef
import com.metrolist.music.beat.BeatMixKind
import com.metrolist.music.beat.BeatMixLineup
import com.metrolist.music.beat.BeatMixRepository
import com.metrolist.music.beat.BeatSmartShuffle
import com.metrolist.music.beat.normalizeArtist
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.BeatMixQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MediaMetadataListItem
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.LocalBeatLook
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** Page with every mix Beat proposes. */
const val BEAT_MIXES_ROUTE = "beat_mixes"

/** Page of one mix, with its songs. */
const val BEAT_MIX_ROUTE = "beat_mix/{mixId}"

fun beatMixRoute(id: String) = "beat_mix/${Uri.encode(id)}"

@HiltViewModel
class BeatMixesViewModel
    @Inject
    constructor(
        val repository: BeatMixRepository,
    ) : ViewModel() {
        val lineup: StateFlow<BeatMixLineup?> = repository.lineup

        private val _changing = MutableStateFlow(false)
        val changing: StateFlow<Boolean> = _changing.asStateFlow()

        init {
            viewModelScope.launch { runCatching { repository.loadLineup() } }
        }

        /** "Cambia i mix": a different selection for today. */
        fun change() {
            if (_changing.value) return
            viewModelScope.launch {
                _changing.value = true
                runCatching { repository.loadLineup(rotate = true) }
                _changing.value = false
            }
        }
    }

data class BeatMixUiState(
    val def: BeatMixDef? = null,
    val content: BeatMixContent? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

@HiltViewModel
class BeatMixViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        val repository: BeatMixRepository,
    ) : ViewModel() {
        private val mixId: String = savedStateHandle.get<String>("mixId").orEmpty()

        private val _state = MutableStateFlow(BeatMixUiState(content = repository.cached(mixId)))
        val state: StateFlow<BeatMixUiState> = _state.asStateFlow()

        init {
            load(refresh = false)
        }

        /** Loads the mix; [refresh] builds a brand new selection of songs. */
        fun load(refresh: Boolean) {
            viewModelScope.launch {
                _state.value = _state.value.copy(loading = true, failed = false)
                val def = runCatching { repository.findDef(mixId) }.getOrNull()
                if (def == null) {
                    _state.value = _state.value.copy(loading = false, failed = true)
                    return@launch
                }
                _state.value = _state.value.copy(def = def)
                runCatching { repository.content(def, refresh) }
                    .onSuccess { _state.value = BeatMixUiState(def = def, content = it, loading = false) }
                    .onFailure { _state.value = _state.value.copy(loading = false, failed = true) }
            }
        }
    }

/**
 * Plays a mix. With [shuffle] the songs are reordered again (artists kept apart, new and known songs alternated),
 * otherwise the mix starts from [startIndex] in the order shown.
 */
fun PlayerConnection.playBeatMix(
    content: BeatMixContent,
    startIndex: Int = 0,
    shuffle: Boolean = false,
) {
    val tracks =
        if (shuffle) {
            val byId = content.tracks.associateBy { it.metadata.id }
            BeatSmartShuffle
                .shuffle(
                    content.tracks.map { track ->
                        BeatCandidate(track.metadata.id, track.metadata.artists.map { normalizeArtist(it.name) }, known = !track.isNew)
                    },
                    Random(System.nanoTime()),
                ).mapNotNull { byId[it.id] }
        } else {
            content.tracks
        }
    if (tracks.isEmpty()) return
    val continueFrom = (tracks.filter { it.isNew }.ifEmpty { tracks }).random().metadata.id
    playQueue(
        BeatMixQueue(
            title = content.def.title,
            items = tracks.map { it.metadata.toMediaItem() },
            startIndex = if (shuffle) 0 else startIndex,
            continueFromId = continueFrom,
        ),
    )
}

private fun MediaMetadata.toSongItem() =
    SongItem(
        id = id,
        title = title,
        artists = artists.map { com.metrolist.innertube.models.Artist(name = it.name, id = it.id) },
        album = album?.let { com.metrolist.innertube.models.Album(name = it.title, id = it.id) },
        duration = duration.takeIf { it > 0 },
        musicVideoType = musicVideoType,
        thumbnail = thumbnailUrl.orEmpty(),
        explicit = explicit,
    )

private fun kindLabel(kind: BeatMixKind): String =
    when (kind) {
        BeatMixKind.DAILY -> "Daily"
        BeatMixKind.GENRE -> "Genere"
        BeatMixKind.ORIGINAL -> "Il tuo mix"
        BeatMixKind.ARTIST -> "Artista"
        BeatMixKind.SIMILAR -> "Artisti simili"
        BeatMixKind.REDISCOVER -> "Riscoperta"
        BeatMixKind.DISCOVER -> "Novità"
        BeatMixKind.OUTSIDE -> "Da scoprire"
    }

private fun BeatMixDef.brush() = Brush.linearGradient(listOf(Color(colors.first), Color(colors.second)))

/** Shared "play straight from the card" logic: builds the mix if needed, then plays it. */
@Composable
private fun rememberMixPlayer(repository: BeatMixRepository): Pair<String?, (BeatMixDef) -> Unit> {
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loadingId by remember { mutableStateOf<String?>(null) }
    val errorText = stringResource(R.string.beat_mix_error)
    return loadingId to { def: BeatMixDef ->
        if (loadingId == null && playerConnection != null) {
            loadingId = def.id
            scope.launch {
                runCatching { repository.content(def) }
                    .onSuccess { playerConnection.playBeatMix(it, shuffle = true) }
                    .onFailure { Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show() }
                loadingId = null
            }
        }
    }
}

/**
 * The mixes on the home: a big "Mix del giorno", the mixes made for today, the discoveries
 * and the user's original mixes in a compact row. Everything else is in "Tutti i mix".
 */
@Composable
fun BeatMixHomeSection(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val viewModel: BeatMixesViewModel = hiltViewModel()
    val lineup by viewModel.lineup.collectAsStateWithLifecycle()
    val changing by viewModel.changing.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val (loadingId, play) = rememberMixPlayer(viewModel.repository)
    val open: (BeatMixDef) -> Unit = { navController.navigate(beatMixRoute(it.id)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.beat_mixes_header),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::change, enabled = !changing) {
                if (changing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(painterResource(R.drawable.refresh), contentDescription = stringResource(R.string.beat_mixes_change))
                }
            }
            TextButton(onClick = { navController.navigate(BEAT_MIXES_ROUTE) }) {
                Text(stringResource(R.string.beat_mixes_all))
            }
        }

        val current = lineup
        if (current == null) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        current.hero?.let { hero ->
            BeatMixHeroCard(
                def = hero,
                pictures = rememberMixCover(hero, viewModel.repository),
                loading = loadingId == hero.id,
                onOpen = { open(hero) },
                onPlay = { play(hero) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        BeatMixRow(stringResource(R.string.beat_mixes_made_for_you), current.forYou, viewModel.repository, loadingId, open, play)
        BeatMixRow(stringResource(R.string.beat_mixes_discover), current.discover, viewModel.repository, loadingId, open, play)
        if (expanded) {
            BeatMixRow(stringResource(R.string.beat_mixes_artists), current.artistMixes, viewModel.repository, loadingId, open, play)
            BeatMixRow(stringResource(R.string.beat_mixes_genres), current.genres, viewModel.repository, loadingId, open, play)
        }
        BeatMixRow(stringResource(R.string.beat_mixes_mine), current.originals, viewModel.repository, loadingId, open, play, cardSize = 112.dp)
    }
}

/** Cover pictures of a mix, loaded in the background and renewed when the repository changes them. */
@Composable
private fun rememberMixCover(
    def: BeatMixDef,
    repository: BeatMixRepository,
): List<String> {
    val version by repository.coverVersion.collectAsStateWithLifecycle()
    val pictures by produceState(repository.cachedCover(def.id).orEmpty(), def.id, version) {
        value = repository.cover(def)
    }
    return pictures
}

private val ArtistCoverKinds = setOf(BeatMixKind.DAILY, BeatMixKind.ARTIST, BeatMixKind.SIMILAR)

/**
 * Spotify-style cover: artist photos in circles for artist mixes, a big album cover for the others,
 * on the mix colours, with the title on a coloured band at the bottom.
 */
@Composable
fun BeatMixCover(
    def: BeatMixDef,
    pictures: List<String>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.background(def.brush())) {
        val side = maxWidth
        val band = maxHeight * 0.3f
        val bandColor = Color(def.colors.first)
        val textColor = if (bandColor.luminance() > 0.5f) Color.Black else Color.White
        val artArea = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(maxHeight - band)
        if (pictures.isEmpty()) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = artArea.padding(side * 0.18f),
            )
        } else if (def.kind in ArtistCoverKinds) {
            Box(contentAlignment = Alignment.Center, modifier = artArea.padding(top = side * 0.04f)) {
                val small = side * 0.34f
                pictures.getOrNull(1)?.let { CoverCircle(it, small, Modifier.offset(x = -side * 0.27f)) }
                pictures.getOrNull(2)?.let { CoverCircle(it, small, Modifier.offset(x = side * 0.27f)) }
                CoverCircle(pictures[0], side * 0.46f)
            }
        } else {
            Box(contentAlignment = Alignment.Center, modifier = artArea.padding(top = side * 0.05f)) {
                AsyncImage(
                    model = pictures[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(side * 0.6f)
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                )
            }
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(band)
                    .background(bandColor)
                    .padding(horizontal = side * 0.07f),
        ) {
            Text(
                text = def.title,
                color = textColor,
                fontSize = (side.value * 0.095f).sp,
                lineHeight = (side.value * 0.105f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CoverCircle(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .size(size)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f)),
    )
}

@Composable
private fun BeatMixRow(
    title: String,
    mixes: List<BeatMixDef>,
    repository: BeatMixRepository,
    loadingId: String?,
    onOpen: (BeatMixDef) -> Unit,
    onPlay: (BeatMixDef) -> Unit,
    cardSize: Dp = 150.dp,
) {
    if (mixes.isEmpty()) return
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(mixes, key = { it.id }) { mix ->
            BeatMixCard(
                def = mix,
                pictures = rememberMixCover(mix, repository),
                loading = loadingId == mix.id,
                onOpen = { onOpen(mix) },
                onPlay = { onPlay(mix) },
                modifier = Modifier.width(cardSize),
            )
        }
    }
}

/** A mix card: square cover with a quick play button, and the description under it. */
@Composable
private fun BeatMixCard(
    def: BeatMixDef,
    pictures: List<String>,
    loading: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = true,
) {
    Column(modifier = modifier.clickable(onClick = onOpen)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(LocalBeatLook.current.cardShape(10.dp)),
        ) {
            BeatMixCover(def, pictures, Modifier.fillMaxSize())
            BeatMixPlayButton(
                loading = loading,
                onClick = onPlay,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                size = 30.dp,
            )
        }
        if (showSubtitle) {
            Text(
                text = def.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp, end = 2.dp),
            )
        }
    }
}

@Composable
private fun BeatMixPlayButton(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(enabled = !loading, onClick = onClick),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(size * 0.55f))
        } else {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = stringResource(R.string.beat_mix_play),
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/** "Mix del giorno": cover on the left, text and a big play button on the right. */
@Composable
private fun BeatMixHeroCard(
    def: BeatMixDef,
    pictures: List<String>,
    loading: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(LocalBeatLook.current.cardShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(def.colors.first).copy(alpha = 0.55f), Color(def.colors.second).copy(alpha = 0.35f)),
                    ),
                ).clickable(onClick = onOpen)
                .padding(12.dp),
    ) {
        BeatMixCover(
            def = def,
            pictures = pictures,
            modifier =
                Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.beat_mix_of_the_day).uppercase(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = def.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = def.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            BeatMixPlayButton(loading = loading, onClick = onPlay, size = 44.dp)
        }
    }
}

/** "Tutti i mix": every mix grouped by type, two per row. */
@Composable
fun BeatMixLibraryScreen(onBack: () -> Unit) {
    val viewModel: BeatMixesViewModel = hiltViewModel()
    val lineup by viewModel.lineup.collectAsStateWithLifecycle()
    val changing by viewModel.changing.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val (loadingId, play) = rememberMixPlayer(viewModel.repository)
    val open: (BeatMixDef) -> Unit = { navController.navigate(beatMixRoute(it.id)) }

    val sections =
        lineup?.let { current ->
            listOf(
                R.string.beat_mixes_made_for_you to (listOfNotNull(current.hero) + current.forYou).distinctBy { it.id },
                R.string.beat_mixes_discover to current.discover,
                R.string.beat_mixes_artists to current.artistMixes,
                R.string.beat_mixes_genres to current.genres,
                R.string.beat_mixes_mine to current.originals,
                R.string.beat_mixes_more_genres to current.moreGenres,
            ).filter { it.second.isNotEmpty() }
        }.orEmpty()

    LazyColumn(
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
                .asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.dismiss))
                }
                Text(
                    text = stringResource(R.string.beat_mixes_all),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::change, enabled = !changing) {
                    if (changing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(painterResource(R.drawable.refresh), contentDescription = stringResource(R.string.beat_mixes_change))
                    }
                }
            }
        }
        if (lineup == null) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
        sections.forEach { (titleRes, mixes) ->
            item(key = "title_$titleRes") {
                Text(
                    text = stringResource(titleRes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            items(mixes.chunked(2), key = { row -> "${titleRes}_${row.first().id}" }) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    row.forEach { mix ->
                        Box(Modifier.weight(1f)) {
                            BeatMixCard(
                                def = mix,
                                pictures = rememberMixCover(mix, viewModel.repository),
                                loading = loadingId == mix.id,
                                onOpen = { open(mix) },
                                onPlay = { play(mix) },
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One mix, like a playlist: cover, play / shuffle / new songs buttons and every song, each one playable. */
@Composable
fun BeatMixScreen(onBack: () -> Unit) {
    val viewModel: BeatMixViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val currentId by (playerConnection?.mediaMetadata ?: remember { MutableStateFlow<MediaMetadata?>(null) }).collectAsStateWithLifecycle()
    val isPlaying by (playerConnection?.isPlaying ?: remember { MutableStateFlow(false) }).collectAsStateWithLifecycle()
    val content = state.content
    val def = state.def ?: content?.def
    val context = LocalContext.current
    val regenerated = stringResource(R.string.beat_mix_regenerated)
    var announceNext by remember { mutableStateOf(false) }
    LaunchedEffect(content?.builtAt) {
        if (announceNext && content != null) {
            Toast.makeText(context, regenerated, Toast.LENGTH_SHORT).show()
            announceNext = false
        }
    }

    LazyColumn(
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                .add(WindowInsets.statusBars)
                .asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.dismiss))
                    }
                }
                if (def != null) {
                    val pictures = rememberMixCover(def, viewModel.repository)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        BeatMixCover(
                            def = def,
                            pictures = pictures,
                            modifier =
                                Modifier
                                    .size(230.dp)
                                    .shadow(16.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Text(
                        text = kindLabel(def.kind).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                    Text(
                        text = def.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    val withArtists =
                        content?.tracks
                            ?.mapNotNull { it.metadata.artists.firstOrNull()?.name }
                            ?.distinct()
                            ?.take(3)
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { stringResource(R.string.beat_mix_with_artists, it.joinToString(", ")) }
                    Text(
                        text = withArtists ?: def.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                    )
                }
                if (content != null) {
                    Text(
                        text = stringResource(R.string.beat_mix_info, content.tracks.size, content.newCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Button(
                            onClick = { playerConnection?.playBeatMix(content) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(painterResource(R.drawable.play), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.beat_mix_play), maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { playerConnection?.playBeatMix(content, shuffle = true) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(painterResource(R.drawable.shuffle), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.beat_mix_shuffle), maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = {
                                announceNext = true
                                viewModel.load(refresh = true)
                            },
                            enabled = !state.loading,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.refresh),
                                contentDescription = stringResource(R.string.beat_mix_regenerate),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (state.loading) {
                    if (content == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.beat_mix_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    }
                } else if (state.failed && content == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    ) {
                        Text(
                            stringResource(R.string.beat_mix_error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load(refresh = true) }) { Text(stringResource(R.string.beat_mix_retry)) }
                    }
                }
            }
        }

        if (content != null) {
            itemsIndexed(content.tracks, key = { _, track -> track.metadata.id }) { index, track ->
                val active = currentId?.id == track.metadata.id
                MediaMetadataListItem(
                    mediaMetadata = track.metadata,
                    isActive = active,
                    isPlaying = active && isPlaying,
                    trailingContent = {
                        if (track.isNew) BeatNewBadge()
                        IconButton(
                            onClick = {
                                menuState.show {
                                    YouTubeSongMenu(song = track.metadata.toSongItem(), onDismiss = menuState::dismiss)
                                }
                            },
                        ) {
                            Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                        }
                    },
                    modifier =
                        Modifier.clickable {
                            if (active) playerConnection?.togglePlayPause() else playerConnection?.playBeatMix(content, startIndex = index)
                        },
                )
            }
        }
    }
}

@Composable
private fun BeatNewBadge() {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.beat_mix_new_badge),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}
