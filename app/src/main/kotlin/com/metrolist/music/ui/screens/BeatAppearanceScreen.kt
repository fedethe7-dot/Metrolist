/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.BeatThemePickerDialog
import com.metrolist.music.ui.theme.BEAT_DEFAULT_GLASS_BLUR
import com.metrolist.music.ui.theme.BEAT_DEFAULT_GLASS_OPACITY
import com.metrolist.music.ui.theme.BEAT_DEFAULT_TEXT_SCALE
import com.metrolist.music.ui.theme.BeatCustomColorAKey
import com.metrolist.music.ui.theme.BeatCustomColorBKey
import com.metrolist.music.ui.theme.BeatFont
import com.metrolist.music.ui.theme.BeatFontKey
import com.metrolist.music.ui.theme.BeatGlassBlurKey
import com.metrolist.music.ui.theme.BeatGlassOpacityKey
import com.metrolist.music.ui.theme.BeatHomeMixesKey
import com.metrolist.music.ui.theme.BeatHomeQuickPicksKey
import com.metrolist.music.ui.theme.BeatHomeShortcutsKey
import com.metrolist.music.ui.theme.BeatHomeSuggestionsKey
import com.metrolist.music.ui.theme.BeatLook
import com.metrolist.music.ui.theme.BeatSpeed
import com.metrolist.music.ui.theme.BeatSpeedKey
import com.metrolist.music.ui.theme.BeatStyle
import com.metrolist.music.ui.theme.BeatStyleKey
import com.metrolist.music.ui.theme.BeatTextScaleKey
import com.metrolist.music.ui.theme.BeatThemeKey
import com.metrolist.music.ui.theme.BeatThemes
import com.metrolist.music.ui.theme.BeatTransition
import com.metrolist.music.ui.theme.BeatTransitionKey
import com.metrolist.music.ui.theme.LocalBeatLook
import com.metrolist.music.ui.theme.beatBackdrop
import com.metrolist.music.ui.theme.beatSurface
import com.metrolist.music.ui.theme.family
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

/** Route of the "Aspetto" page: style, colors, text, effects, animations and home sections. */
const val BEAT_APPEARANCE_ROUTE = "beat_appearance"

private data class StyleOption(val style: BeatStyle, @StringRes val name: Int, @StringRes val hint: Int)

private val StyleOptions = listOf(
    StyleOption(BeatStyle.CLASSIC, R.string.beat_style_classic, R.string.beat_style_classic_hint),
    StyleOption(BeatStyle.GLASS, R.string.beat_style_glass, R.string.beat_style_glass_hint),
    StyleOption(BeatStyle.NEON, R.string.beat_style_neon, R.string.beat_style_neon_hint),
    StyleOption(BeatStyle.BENTO, R.string.beat_style_bento, R.string.beat_style_bento_hint),
)

private val FontOptions = listOf(
    BeatFont.NUNITO to R.string.beat_font_nunito,
    BeatFont.GROTESK to R.string.beat_font_grotesk,
    BeatFont.MANROPE to R.string.beat_font_manrope,
    BeatFont.SYSTEM to R.string.beat_font_system,
)

private val TransitionOptions = listOf(
    BeatTransition.SLIDE to R.string.beat_transition_slide,
    BeatTransition.FADE to R.string.beat_transition_fade,
    BeatTransition.ZOOM to R.string.beat_transition_zoom,
    BeatTransition.GLASS to R.string.beat_transition_glass,
    BeatTransition.WARP to R.string.beat_transition_warp,
)

private val SpeedOptions = listOf(
    BeatSpeed.SLOW to R.string.beat_speed_slow,
    BeatSpeed.NORMAL to R.string.beat_speed_normal,
    BeatSpeed.FAST to R.string.beat_speed_fast,
    BeatSpeed.OFF to R.string.beat_speed_off,
)

@Composable
fun BeatAppearanceScreen(onBack: () -> Unit) {
    val look = LocalBeatLook.current
    val colors = MaterialTheme.colorScheme

    var style by rememberEnumPreference(BeatStyleKey, defaultValue = BeatStyle.CLASSIC)
    var font by rememberEnumPreference(BeatFontKey, defaultValue = BeatFont.NUNITO)
    var savedTextScale by rememberPreference(BeatTextScaleKey, defaultValue = BEAT_DEFAULT_TEXT_SCALE)
    var savedBlur by rememberPreference(BeatGlassBlurKey, defaultValue = BEAT_DEFAULT_GLASS_BLUR)
    var savedOpacity by rememberPreference(BeatGlassOpacityKey, defaultValue = BEAT_DEFAULT_GLASS_OPACITY)
    var transition by rememberEnumPreference(BeatTransitionKey, defaultValue = BeatTransition.SLIDE)
    var speed by rememberEnumPreference(BeatSpeedKey, defaultValue = BeatSpeed.NORMAL)
    var showShortcuts by rememberPreference(BeatHomeShortcutsKey, defaultValue = true)
    var showQuickPicks by rememberPreference(BeatHomeQuickPicksKey, defaultValue = true)
    var showMixes by rememberPreference(BeatHomeMixesKey, defaultValue = true)
    var showSuggestions by rememberPreference(BeatHomeSuggestionsKey, defaultValue = true)
    var themeId by rememberPreference(BeatThemeKey, defaultValue = BeatThemes.Default.id)
    var customA by rememberPreference(BeatCustomColorAKey, defaultValue = BeatThemes.DefaultCustomA.toArgb())
    var customB by rememberPreference(BeatCustomColorBKey, defaultValue = BeatThemes.DefaultCustomB.toArgb())

    // Sliders move freely and save once released, so the whole app is not redrawn at every step.
    var textScale by remember { mutableFloatStateOf(savedTextScale) }
    var blur by remember { mutableFloatStateOf(savedBlur) }
    var opacity by remember { mutableFloatStateOf(savedOpacity) }
    LaunchedEffect(savedTextScale) { textScale = savedTextScale }
    LaunchedEffect(savedBlur) { blur = savedBlur }
    LaunchedEffect(savedOpacity) { opacity = savedOpacity }

    var showThemes by remember { mutableStateOf(false) }
    if (showThemes) {
        BeatThemePickerDialog(
            selectedId = themeId,
            customA = Color(customA),
            customB = Color(customB),
            onSelect = { themeId = it.id },
            onSaveCustom = { a, b ->
                customA = a.toArgb()
                customB = b.toArgb()
                themeId = BeatThemes.CUSTOM_ID
            },
            onDismiss = { showThemes = false },
        )
    }
    val currentTheme = remember(themeId, customA, customB) { BeatThemes.resolve(themeId, customA, customB) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.dismiss),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.beat_appearance),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        AppearanceSection(title = stringResource(R.string.beat_style), look = look) {
            StyleOptions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { option ->
                        StyleCard(
                            option = option,
                            look = look,
                            selected = style == option.style,
                            onClick = { style = option.style },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        AppearanceSection(title = stringResource(R.string.beat_themes_title), look = look) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { showThemes = true },
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(currentTheme.gradient.ifEmpty { listOf(colors.primary, colors.tertiary) }.let { if (it.size == 1) it + it else it })),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(currentTheme.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.beat_change),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
        }

        AppearanceSection(title = stringResource(R.string.beat_text), look = look) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FontOptions.forEach { (option, label) ->
                    AppearanceChip(
                        text = stringResource(label),
                        selected = font == option,
                        look = look,
                        fontFamily = option.family(),
                        onClick = { font = option },
                    )
                }
            }
            Text(
                text = stringResource(R.string.beat_text_size, (textScale * 100).roundToInt()),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = textScale,
                    onValueChange = { textScale = it },
                    onValueChangeFinished = { savedTextScale = textScale },
                    valueRange = 0.85f..1.25f,
                    steps = 7,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text("A", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (style == BeatStyle.GLASS) {
            AppearanceSection(title = stringResource(R.string.beat_glass_effects), look = look) {
                Text(
                    text = stringResource(R.string.beat_blur, (blur * 100).roundToInt()),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = blur,
                    onValueChange = { blur = it },
                    onValueChangeFinished = { savedBlur = blur },
                )
                Text(
                    text = stringResource(R.string.beat_transparency, (opacity * 100).roundToInt()),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    onValueChangeFinished = { savedOpacity = opacity },
                )
                Text(
                    text = stringResource(R.string.beat_blur_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        AppearanceSection(title = stringResource(R.string.beat_animations), look = look) {
            Text(text = stringResource(R.string.beat_page_transition), style = MaterialTheme.typography.titleSmall)
            TransitionOptions.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (option, label) ->
                        AppearanceChip(
                            text = stringResource(label),
                            selected = transition == option,
                            look = look,
                            onClick = { transition = option },
                        )
                    }
                }
            }
            Text(text = stringResource(R.string.beat_speed), style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SpeedOptions.forEach { (option, label) ->
                    AppearanceChip(
                        text = stringResource(label),
                        selected = speed == option,
                        look = look,
                        onClick = { speed = option },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        AppearanceSection(title = stringResource(R.string.beat_home_screen), look = look) {
            SwitchRow(stringResource(R.string.beat_home_shortcuts), showShortcuts) { showShortcuts = it }
            SwitchRow(stringResource(R.string.beat_home_quick_picks), showQuickPicks) { showQuickPicks = it }
            SwitchRow(stringResource(R.string.beat_home_mixes), showMixes) { showMixes = it }
            SwitchRow(stringResource(R.string.beat_home_suggestions), showSuggestions) { showSuggestions = it }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .beatSurface(look, look.cardShape(), colors.surfaceContainerHigh)
                .clickable(role = Role.Button) {
                    style = BeatStyle.CLASSIC
                    font = BeatFont.NUNITO
                    savedTextScale = BEAT_DEFAULT_TEXT_SCALE
                    savedBlur = BEAT_DEFAULT_GLASS_BLUR
                    savedOpacity = BEAT_DEFAULT_GLASS_OPACITY
                    transition = BeatTransition.SLIDE
                    speed = BeatSpeed.NORMAL
                    showShortcuts = true
                    showQuickPicks = true
                    showMixes = true
                    showSuggestions = true
                },
        ) {
            Text(text = stringResource(R.string.beat_reset), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AppearanceSection(
    title: String,
    look: BeatLook,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .beatSurface(look, look.cardShape(), MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** A small live sample of each style, drawn with the current theme colors. */
@Composable
private fun StyleCard(
    option: StyleOption,
    look: BeatLook,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = look.copy(style = option.style)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(12.dp))
                .beatBackdrop(preview),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
                    .height(18.dp)
                    .beatSurface(preview, preview.pillShape(), colors.surfaceContainerHighest),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .beatSurface(preview, preview.cardShape(10.dp), colors.primary.copy(alpha = 0.5f), emphasis = true),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .beatSurface(preview, preview.cardShape(10.dp), colors.surfaceContainerHighest),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(option.name),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(option.hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppearanceChip(
    text: String,
    selected: Boolean,
    look: BeatLook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(if (look.style == BeatStyle.NEON) 10.dp else 20.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .beatSurface(
                look = look,
                shape = shape,
                fallback = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
                emphasis = selected,
            )
            .then(if (selected && look.isClassic) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            color = if (selected && look.isClassic) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Switch) { onChange(!checked) },
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
