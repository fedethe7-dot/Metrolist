/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.R

/** Nunito, the rounded face shared with the Garage and Palestra apps (SIL Open Font License). */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/** One variable font file mapped to the weights the app uses. */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, vararg weights: Pair<FontWeight, Int>) = FontFamily(
    weights.map { (weight, axis) ->
        Font(resId, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(axis)))
    },
)

/** Space Grotesk, technical and squared: the neon look (SIL Open Font License). */
val SpaceGrotesk = variableFamily(
    R.font.spacegrotesk_variable,
    FontWeight.Normal to 400,
    FontWeight.Medium to 500,
    FontWeight.SemiBold to 600,
    FontWeight.Bold to 700,
    FontWeight.ExtraBold to 700,
)

/** Manrope, geometric and bold: the bento look (SIL Open Font License). */
val Manrope = variableFamily(
    R.font.manrope_variable,
    FontWeight.Normal to 400,
    FontWeight.Medium to 500,
    FontWeight.SemiBold to 600,
    FontWeight.Bold to 700,
    FontWeight.ExtraBold to 800,
)

fun BeatFont.family(): FontFamily = when (this) {
    BeatFont.NUNITO -> Nunito
    BeatFont.GROTESK -> SpaceGrotesk
    BeatFont.MANROPE -> Manrope
    BeatFont.SYSTEM -> FontFamily.Default
}

private fun TextStyle.withFamily(family: FontFamily, weight: FontWeight? = null) =
    copy(fontFamily = family, fontWeight = weight ?: fontWeight)

/**
 * Material type scale in [family]. Display and headline roles run heavier because rounded faces
 * read thin at Material's default weights; body and label sizes stay untouched.
 */
fun beatTypography(family: FontFamily): Typography = Typography().run {
    copy(
        displayLarge = displayLarge.withFamily(family, FontWeight.ExtraBold),
        displayMedium = displayMedium.withFamily(family, FontWeight.ExtraBold),
        displaySmall = displaySmall.withFamily(family, FontWeight.ExtraBold),
        headlineLarge = headlineLarge.withFamily(family, FontWeight.ExtraBold),
        headlineMedium = headlineMedium.withFamily(family, FontWeight.ExtraBold),
        headlineSmall = headlineSmall.withFamily(family, FontWeight.Bold),
        titleLarge = titleLarge.withFamily(family, FontWeight.Bold),
        titleMedium = titleMedium.withFamily(family, FontWeight.SemiBold),
        titleSmall = titleSmall.withFamily(family, FontWeight.SemiBold),
        bodyLarge = bodyLarge.withFamily(family),
        bodyMedium = bodyMedium.withFamily(family),
        bodySmall = bodySmall.withFamily(family),
        labelLarge = labelLarge.withFamily(family, FontWeight.Bold),
        labelMedium = labelMedium.withFamily(family, FontWeight.SemiBold),
        labelSmall = labelSmall.withFamily(family, FontWeight.SemiBold),
    )
}

val BeatTypography: Typography = beatTypography(Nunito)
