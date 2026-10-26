/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.R

/** Selected "And the beat goes on" theme, stored by [BeatTheme.id]. */
val BeatThemeKey = stringPreferencesKey("beatTheme")

/** The two colors of the "Personalizzato" theme, stored as ARGB ints. */
val BeatCustomColorAKey = intPreferencesKey("beatCustomColorA")
val BeatCustomColorBKey = intPreferencesKey("beatCustomColorB")

/**
 * Hand-tuned dark themes. [seed] drives the Material color scheme; [background], [surface]
 * and [accent] replace the generated tones so each theme keeps its own deep, saturated look;
 * [gradient] is used for the soft wash at the top of the home screen and in the theme picker.
 * A theme with [followsCover] takes its colors from the song that is playing.
 */
data class BeatTheme(
    val id: String,
    @StringRes val nameRes: Int,
    val seed: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val gradient: List<Color>,
    val followsCover: Boolean = false,
)

object BeatThemes {
    val Cover = BeatTheme(
        id = "cover",
        nameRes = R.string.beat_theme_cover,
        seed = Color(0xFF7C9CFF),
        accent = Color(0xFF9DB4FF),
        background = Color(0xFF0D0F14),
        surface = Color(0xFF161922),
        gradient = listOf(Color(0xFFFF6B9A), Color(0xFF7C9CFF), Color(0xFF3DDC97)),
        followsCover = true,
    )

    val NightBlue = BeatTheme(
        id = "notte_blu",
        nameRes = R.string.beat_theme_night_blue,
        seed = Color(0xFF4C7DFF),
        accent = Color(0xFF7FA2FF),
        background = Color(0xFF0A0F1F),
        surface = Color(0xFF131A30),
        gradient = listOf(Color(0xFF3B5BFF), Color(0xFF8A5CFF)),
    )

    val Sunset = BeatTheme(
        id = "tramonto",
        nameRes = R.string.beat_theme_sunset,
        seed = Color(0xFFFF7A59),
        accent = Color(0xFFFF9A76),
        background = Color(0xFF160B12),
        surface = Color(0xFF24121C),
        gradient = listOf(Color(0xFFFF8A3D), Color(0xFFFF4F8B), Color(0xFF8E44FF)),
    )

    val Emerald = BeatTheme(
        id = "smeraldo",
        nameRes = R.string.beat_theme_emerald,
        seed = Color(0xFF1FC8A0),
        accent = Color(0xFF4FE3BD),
        background = Color(0xFF06120F),
        surface = Color(0xFF0F201B),
        gradient = listOf(Color(0xFF11C77E), Color(0xFF18B5C9)),
    )

    val RedGold = BeatTheme(
        id = "rosso_oro",
        nameRes = R.string.beat_theme_red_gold,
        seed = Color(0xFFD6362F),
        accent = Color(0xFFFFC542),
        background = Color(0xFF140708),
        surface = Color(0xFF241011),
        gradient = listOf(Color(0xFFB3191B), Color(0xFFF2A81D)),
    )

    val OrangeBlue = BeatTheme(
        id = "arancio_blu",
        nameRes = R.string.beat_theme_orange_blue,
        seed = Color(0xFFFF8C1A),
        accent = Color(0xFFFFA347),
        background = Color(0xFF0C0F1A),
        surface = Color(0xFF171C2E),
        gradient = listOf(Color(0xFFFF7A00), Color(0xFF1E4FD8)),
    )

    val RacingRed = BeatTheme(
        id = "rosso_corsa",
        nameRes = R.string.beat_theme_racing_red,
        seed = Color(0xFFE10600),
        accent = Color(0xFFFF4D4D),
        background = Color(0xFF0E0E0E),
        surface = Color(0xFF1C1C1C),
        gradient = listOf(Color(0xFFE10600), Color(0xFF5A0000)),
    )

    val Silver = BeatTheme(
        id = "argento",
        nameRes = R.string.beat_theme_silver,
        seed = Color(0xFF9AA7B4),
        accent = Color(0xFF00D2BE),
        background = Color(0xFF0B0D0F),
        surface = Color(0xFF191C20),
        gradient = listOf(Color(0xFF8E9AA6), Color(0xFF00A99A)),
    )

    val Purple = BeatTheme(
        id = "galassia",
        nameRes = R.string.beat_theme_galaxy,
        seed = Color(0xFF9B5CFF),
        accent = Color(0xFFC08BFF),
        background = Color(0xFF0D0818),
        surface = Color(0xFF1A1230),
        gradient = listOf(Color(0xFF5B2DFF), Color(0xFFE040FB)),
    )

    val Ocean = BeatTheme(
        id = "oceano",
        nameRes = R.string.beat_theme_ocean,
        seed = Color(0xFF00B4D8),
        accent = Color(0xFF48CAE4),
        background = Color(0xFF041218),
        surface = Color(0xFF0B2029),
        gradient = listOf(Color(0xFF0077B6), Color(0xFF00D4C8)),
    )

    val NeonPink = BeatTheme(
        id = "rosa_neon",
        nameRes = R.string.beat_theme_neon_pink,
        seed = Color(0xFFFF3D9A),
        accent = Color(0xFFFF6FB5),
        background = Color(0xFF14060F),
        surface = Color(0xFF26101E),
        gradient = listOf(Color(0xFFFF2E93), Color(0xFF7B2CFF)),
    )

    val Lime = BeatTheme(
        id = "lime",
        nameRes = R.string.beat_theme_lime,
        seed = Color(0xFF9BE15D),
        accent = Color(0xFFB8F27C),
        background = Color(0xFF0B1206),
        surface = Color(0xFF17220F),
        gradient = listOf(Color(0xFF9BE15D), Color(0xFF00C27A)),
    )

    val Amber = BeatTheme(
        id = "ambra",
        nameRes = R.string.beat_theme_amber,
        seed = Color(0xFFFFB020),
        accent = Color(0xFFFFC94D),
        background = Color(0xFF140E04),
        surface = Color(0xFF241A0B),
        gradient = listOf(Color(0xFFFFB020), Color(0xFFE2541B)),
    )

    val Ice = BeatTheme(
        id = "ghiaccio",
        nameRes = R.string.beat_theme_ice,
        seed = Color(0xFF8EC5FF),
        accent = Color(0xFFB5DAFF),
        background = Color(0xFF080D14),
        surface = Color(0xFF131B26),
        gradient = listOf(Color(0xFFB5DAFF), Color(0xFF5E7CE2)),
    )

    val Midnight = BeatTheme(
        id = "mezzanotte",
        nameRes = R.string.beat_theme_midnight,
        seed = Color(0xFF3F51B5),
        accent = Color(0xFF7986FF),
        background = Color(0xFF05060F),
        surface = Color(0xFF10132A),
        gradient = listOf(Color(0xFF1A237E), Color(0xFF00897B)),
    )

    const val CUSTOM_ID = "personalizzato"
    val DefaultCustomA = Color(0xFFFF5E62)
    val DefaultCustomB = Color(0xFF6A5CFF)

    /** "Personalizzato": the user's two colors drive the gradient, accent and the tint of the surfaces. */
    fun custom(a: Color, b: Color): BeatTheme = BeatTheme(
        id = CUSTOM_ID,
        nameRes = R.string.beat_theme_custom,
        seed = a,
        accent = lerpColor(a, Color.White, 0.25f),
        background = lerpColor(Color(0xFF07080C), a, 0.07f),
        surface = lerpColor(Color(0xFF14161D), a, 0.12f),
        gradient = listOf(a, b),
    )

    val All = listOf(
        NightBlue, Cover, Sunset, Emerald, Purple, RedGold, OrangeBlue, RacingRed, Silver,
        Ocean, NeonPink, Lime, Amber, Ice, Midnight,
    )

    val Default = NightBlue

    fun byId(id: String?): BeatTheme = All.firstOrNull { it.id == id } ?: Default

    fun resolve(id: String?, customA: Int?, customB: Int?): BeatTheme =
        if (id == CUSTOM_ID) {
            custom(customA?.let { Color(it) } ?: DefaultCustomA, customB?.let { Color(it) } ?: DefaultCustomB)
        } else {
            byId(id)
        }
}

/** Applies the theme's deep background and surfaces on top of a generated dark scheme. */
fun ColorScheme.withBeatTheme(theme: BeatTheme, pureBlack: Boolean): ColorScheme {
    val background = if (pureBlack) Color.Black else theme.background
    if (theme.followsCover) {
        return if (pureBlack) copy(background = Color.Black, surface = Color.Black) else this
    }
    return copy(
        primary = theme.accent,
        background = background,
        surface = background,
        surfaceDim = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = lerpColor(background, theme.surface, 0.5f),
        surfaceContainer = theme.surface,
        surfaceContainerHigh = lerpColor(theme.surface, theme.accent, 0.08f),
        surfaceContainerHighest = lerpColor(theme.surface, theme.accent, 0.14f),
    )
}

internal fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
