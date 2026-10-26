/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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

private fun TextStyle.nunito(weight: FontWeight? = null) =
    copy(fontFamily = Nunito, fontWeight = weight ?: fontWeight)

/**
 * Material type scale set in Nunito. Display and headline roles run heavier because a rounded face
 * reads thin at Material's default weights; body and label sizes stay untouched.
 */
val BeatTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.nunito(FontWeight.ExtraBold),
        displayMedium = displayMedium.nunito(FontWeight.ExtraBold),
        displaySmall = displaySmall.nunito(FontWeight.ExtraBold),
        headlineLarge = headlineLarge.nunito(FontWeight.ExtraBold),
        headlineMedium = headlineMedium.nunito(FontWeight.ExtraBold),
        headlineSmall = headlineSmall.nunito(FontWeight.Bold),
        titleLarge = titleLarge.nunito(FontWeight.Bold),
        titleMedium = titleMedium.nunito(FontWeight.SemiBold),
        titleSmall = titleSmall.nunito(FontWeight.SemiBold),
        bodyLarge = bodyLarge.nunito(),
        bodyMedium = bodyMedium.nunito(),
        bodySmall = bodySmall.nunito(),
        labelLarge = labelLarge.nunito(FontWeight.Bold),
        labelMedium = labelMedium.nunito(FontWeight.SemiBold),
        labelSmall = labelSmall.nunito(FontWeight.SemiBold),
    )
}
