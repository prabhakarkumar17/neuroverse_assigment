package com.neuroverse.authapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale. Material 3 ships sensible defaults; we override only the few
 * styles this app actually leans on.
 *
 * Note `.sp` not `.dp` for text: sp scales with the user's font-size
 * accessibility setting, dp does not. Sizing text in dp ignores users who have
 * enlarged their system font — use sp for text, always.
 */
val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.merge(
            TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        ),
        headlineMedium = headlineMedium.merge(
            TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp)
        ),
        titleMedium = titleMedium.merge(
            TextStyle(fontWeight = FontWeight.SemiBold)
        ),
        labelLarge = labelLarge.merge(
            TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        )
    )
}
