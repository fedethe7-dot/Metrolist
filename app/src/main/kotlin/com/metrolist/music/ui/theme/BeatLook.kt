/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** How the app is drawn: shapes, surfaces and effects. Colors still come from the [BeatTheme]. */
enum class BeatStyle { CLASSIC, GLASS, NEON, BENTO }

enum class BeatFont { NUNITO, GROTESK, MANROPE, SYSTEM }

enum class BeatTransition { SLIDE, FADE, ZOOM, GLASS, WARP }

/** Multiplier on page-change durations; [OFF] switches them off. */
enum class BeatSpeed(val factor: Float) { SLOW(1.6f), NORMAL(1f), FAST(0.55f), OFF(0f) }

val BeatStyleKey = stringPreferencesKey("beatStyle")
val BeatFontKey = stringPreferencesKey("beatFont")
val BeatTextScaleKey = floatPreferencesKey("beatTextScale")
val BeatGlassBlurKey = floatPreferencesKey("beatGlassBlur")
val BeatGlassOpacityKey = floatPreferencesKey("beatGlassOpacity")
val BeatTransitionKey = stringPreferencesKey("beatTransition")
val BeatSpeedKey = stringPreferencesKey("beatSpeed")
val BeatHomeShortcutsKey = booleanPreferencesKey("beatHomeShortcuts")
val BeatHomeQuickPicksKey = booleanPreferencesKey("beatHomeQuickPicks")
val BeatHomeMixesKey = booleanPreferencesKey("beatHomeMixes")
val BeatHomeSuggestionsKey = booleanPreferencesKey("beatHomeSuggestions")

const val BEAT_DEFAULT_TEXT_SCALE = 1f
const val BEAT_DEFAULT_GLASS_BLUR = 0.7f
const val BEAT_DEFAULT_GLASS_OPACITY = 0.4f

private val NeonBase = Color(0xFF05060B)
private val NeonPanel = Color(0xFF0B0F18)
private val NeonLine = Color(0xFF1F2A3C)
private val BentoPanel = Color(0xFF1C1C1F)

/** Everything a Beat component needs to know to draw itself in the chosen style. */
@Immutable
data class BeatLook(
    val style: BeatStyle = BeatStyle.CLASSIC,
    val accent: Color = Color(0xFF7FA2FF),
    val accent2: Color = Color(0xFF8A5CFF),
    val base: Color = Color(0xFF0A0F1F),
    val glassBlur: Float = BEAT_DEFAULT_GLASS_BLUR,
    val glassOpacity: Float = BEAT_DEFAULT_GLASS_OPACITY,
    val speed: BeatSpeed = BeatSpeed.NORMAL,
) {
    val isClassic: Boolean get() = style == BeatStyle.CLASSIC

    /** Corner radius of cards and panels. */
    fun cardShape(classic: Dp = 22.dp): Shape = RoundedCornerShape(
        when (style) {
            BeatStyle.CLASSIC -> classic
            BeatStyle.GLASS -> 26.dp
            BeatStyle.NEON -> 14.dp
            BeatStyle.BENTO -> 28.dp
        },
    )

    /** Shape of the floating mini player and other pill-like bars. */
    fun pillShape(): Shape = RoundedCornerShape(
        when (style) {
            BeatStyle.NEON -> 18.dp
            else -> 36.dp
        },
    )

    /** Opaque color behind the home search bar, close to the top of [beatBackdrop]. */
    val headerColor: Color
        get() = when (style) {
            BeatStyle.CLASSIC -> base
            BeatStyle.GLASS -> lerp(base, accent, 0.22f)
            BeatStyle.NEON -> NeonBase
            BeatStyle.BENTO -> Color.Black
        }

    /** Opaque color a blurred glass panel sits on, so content under it never shows through sharp. */
    val glassBackground: Color get() = lerp(base, lerp(accent, accent2, 0.5f), 0.18f)
}

val LocalBeatLook = staticCompositionLocalOf { BeatLook() }

/** Shared blur source: the page content, read by glass panels floating above it. */
val LocalBeatHaze = staticCompositionLocalOf<HazeState?> { null }

/** Full-screen background of the style: colored glows for glass, a lit grid for neon, black for bento. */
fun Modifier.beatBackdrop(look: BeatLook): Modifier = when (look.style) {
    BeatStyle.CLASSIC -> background(look.base)
    BeatStyle.BENTO -> background(Color.Black)
    BeatStyle.GLASS -> drawBehind {
        drawRect(look.base)
        drawRect(
            Brush.radialGradient(
                colors = listOf(look.accent.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.02f),
                radius = size.width * 1.1f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(look.accent2.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(size.width, size.height * 0.4f),
                radius = size.width * 0.95f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(lerp(look.accent, look.accent2, 0.5f).copy(alpha = 0.3f), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height),
                radius = size.width * 1.05f,
            ),
        )
    }
    BeatStyle.NEON -> drawWithCache {
        val step = 28.dp.toPx()
        val line = look.accent.copy(alpha = 0.06f)
        val topGlow = Brush.radialGradient(
            colors = listOf(look.accent.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.5f, 0f),
            radius = size.width * 0.95f,
        )
        val bottomGlow = Brush.radialGradient(
            colors = listOf(look.accent2.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height),
            radius = size.width * 0.85f,
        )
        onDrawBehind {
            drawRect(NeonBase)
            var x = 0f
            while (x < size.width) {
                drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            drawRect(topGlow)
            drawRect(bottomGlow)
        }
    }
}

/**
 * Card or panel background in the chosen style. [fallback] is what the classic look paints;
 * [emphasis] makes the panel stand out a little more (selected item, main control).
 */
fun Modifier.beatSurface(
    look: BeatLook,
    shape: Shape,
    fallback: Color,
    emphasis: Boolean = false,
): Modifier = when (look.style) {
    BeatStyle.CLASSIC -> clip(shape).background(fallback)
    BeatStyle.GLASS -> {
        val fill = 0.05f + 0.13f * look.glassOpacity + if (emphasis) 0.08f else 0f
        clip(shape)
            .background(Color.White.copy(alpha = fill))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (emphasis) 0.55f else 0.36f), Color.White.copy(alpha = 0.08f)),
                ),
                shape = shape,
            )
    }
    BeatStyle.NEON ->
        shadow(
            elevation = if (emphasis) 18.dp else 8.dp,
            shape = shape,
            clip = false,
            ambientColor = look.accent,
            spotColor = look.accent,
        )
            .clip(shape)
            .background(if (emphasis) lerp(NeonPanel, look.accent, 0.12f) else NeonPanel)
            .border(1.dp, if (emphasis) look.accent.copy(alpha = 0.85f) else look.accent.copy(alpha = 0.4f), shape)
    BeatStyle.BENTO -> clip(shape).background(if (emphasis) lerp(BentoPanel, look.accent, 0.25f) else BentoPanel)
}

/** Border of the neon panels that are not lit (secondary buttons). */
val BeatNeonLine: Color get() = NeonLine

/** Material shapes for the style, so stock components follow it too. */
fun beatShapes(style: BeatStyle): Shapes = when (style) {
    BeatStyle.CLASSIC -> Shapes()
    BeatStyle.GLASS -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
    BeatStyle.NEON -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(18.dp),
    )
    BeatStyle.BENTO -> Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(34.dp),
    )
}

/** Surfaces of the dark scheme for the style; glass and classic keep the theme's own. */
fun ColorScheme.withBeatStyle(style: BeatStyle, pureBlack: Boolean): ColorScheme = when (style) {
    BeatStyle.NEON -> {
        val base = if (pureBlack) Color.Black else NeonBase
        copy(
            background = base,
            surface = base,
            surfaceDim = base,
            surfaceContainerLowest = base,
            surfaceContainerLow = Color(0xFF080B12),
            surfaceContainer = NeonPanel,
            surfaceContainerHigh = Color(0xFF111827),
            surfaceContainerHighest = Color(0xFF172033),
        )
    }
    BeatStyle.BENTO -> copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0E0E10),
        surfaceContainer = Color(0xFF151517),
        surfaceContainerHigh = BentoPanel,
        surfaceContainerHighest = Color(0xFF26262B),
    )
    else -> this
}

private const val BASE_DURATION = 280
private val Overshoot = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

private fun duration(speed: BeatSpeed, scale: Float = 1f) = (BASE_DURATION * speed.factor * scale).roundToInt().coerceAtLeast(1)

/** Page entering. [forward] is true when moving deeper or to a tab further right. */
fun beatEnterTransition(transition: BeatTransition, speed: BeatSpeed, forward: Boolean): EnterTransition {
    if (speed == BeatSpeed.OFF) return EnterTransition.None
    val d = duration(speed)
    return when (transition) {
        BeatTransition.SLIDE ->
            slideInHorizontally(tween(d, easing = FastOutSlowInEasing)) { if (forward) it / 8 else -it / 8 } +
                fadeIn(tween(d))
        BeatTransition.FADE -> fadeIn(tween(d))
        BeatTransition.ZOOM -> scaleIn(tween(d, easing = FastOutSlowInEasing), initialScale = 0.92f) + fadeIn(tween(d))
        BeatTransition.GLASS -> scaleIn(tween(d, easing = FastOutSlowInEasing), initialScale = 1.06f) + fadeIn(tween(d))
        BeatTransition.WARP ->
            scaleIn(tween(duration(speed, 1.3f), easing = Overshoot), initialScale = 0.86f) +
                slideInVertically(tween(duration(speed, 1.3f), easing = Overshoot)) { it / 12 } +
                fadeIn(tween(d))
    }
}

/** Page leaving, paired with [beatEnterTransition]. */
fun beatExitTransition(transition: BeatTransition, speed: BeatSpeed, forward: Boolean): ExitTransition {
    if (speed == BeatSpeed.OFF) return ExitTransition.None
    val d = duration(speed)
    return when (transition) {
        BeatTransition.SLIDE ->
            slideOutHorizontally(tween(d, easing = FastOutSlowInEasing)) { if (forward) -it / 8 else it / 8 } +
                fadeOut(tween(d))
        BeatTransition.FADE -> fadeOut(tween(d))
        BeatTransition.ZOOM -> scaleOut(tween(d), targetScale = 1.04f) + fadeOut(tween(d))
        BeatTransition.GLASS -> scaleOut(tween(d), targetScale = 0.96f) + fadeOut(tween(d))
        BeatTransition.WARP -> scaleOut(tween(duration(speed, 0.7f)), targetScale = 1.12f) + fadeOut(tween(duration(speed, 0.7f)))
    }
}
