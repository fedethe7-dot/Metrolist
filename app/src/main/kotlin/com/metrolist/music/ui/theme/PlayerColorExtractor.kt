/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlin.math.abs

object PlayerColorExtractor {
    /**
     * Two glow colors from the cover plus a deep base: [glow, secondGlow, base].
     * Very bright or washed-out swatches are toned down so white text stays readable on top.
     */
    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int,
    ): List<Color> {
        val ranked = palette.rankedColors(2, fallbackColor).map { tame(Color(it)) }
        val primary = ranked.first()
        val secondary = ranked.getOrNull(1)?.takeIf { distance(it, primary) > 0.18f } ?: shiftHue(primary, 40f)
        val base = Color(
            red = primary.red * 0.14f,
            green = primary.green * 0.14f,
            blue = primary.blue * 0.14f,
        )
        return listOf(primary, secondary, base)
    }

    private fun tame(color: Color): Color {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.35f)
        hsv[2] = hsv[2].coerceIn(0.45f, 0.85f)
        return Color(AndroidColor.HSVToColor(hsv))
    }

    private fun shiftHue(color: Color, degrees: Float): Color {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        hsv[0] = (hsv[0] + degrees) % 360f
        return Color(AndroidColor.HSVToColor(hsv))
    }

    private fun distance(a: Color, b: Color): Float =
        abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
}
