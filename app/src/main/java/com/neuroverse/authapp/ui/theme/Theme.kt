package com.neuroverse.authapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BrandBlue.copy(alpha = 0.12f),
    secondary = BrandSlate,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00315C),
    primaryContainer = BrandBlueLight.copy(alpha = 0.16f),
    secondary = BrandSlateLight,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorRedLight
)

/** Semantic success colour, which Material 3's scheme has no slot for. */
val successColor: androidx.compose.ui.graphics.Color
    @Composable get() = if (isSystemInDarkTheme()) SuccessGreenLight else SuccessGreen

/**
 * Wraps the whole app. Every Material component below it reads its colours and
 * fonts from here through CompositionLocals, so no screen hardcodes a colour.
 *
 * DYNAMIC COLOUR IS DELIBERATELY OFF. Android 12+ can derive a palette from the
 * user's wallpaper, and for a consumer app that is a nice touch. For a
 * CORPORATE auth app it is wrong: the brand colour would change per device and
 * screenshots would not match the brand guidelines. A considered trade-off, not
 * an omission.
 */
@Composable
fun NeuroVerseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
