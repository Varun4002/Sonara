package com.sonara.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Sonara is intentionally always-dark: it is an ambient environment, not a
// day/night document surface. The reactive background supplies the color.
internal val SonaraColorScheme = darkColorScheme(
    primary = SonaraPrimary,
    onPrimary = SonaraOnPrimary,
    secondary = SonaraAccent,
    onSecondary = SonaraOnPrimary,
    background = SonaraBackground,
    onBackground = SonaraOnBackground,
    surface = SonaraSurface,
    onSurface = SonaraOnBackground,
    surfaceVariant = SonaraSurfaceVariant,
    onSurfaceVariant = SonaraOnSurfaceMuted,
    error = SonaraError,
)

/**
 * Semantic roles M3's [androidx.compose.material3.ColorScheme] does not model.
 * Screens should consume these instead of raw [Color] values so a future
 * artwork-reactive palette has a single injection point.
 */
@Immutable
data class SonaraColors(
    val elevatedSurface: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val playerSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
)

val LocalSonaraColors = staticCompositionLocalOf {
    SonaraColors(
        elevatedSurface = SonaraElevatedSurface,
        glassSurface = SonaraGlassSurface,
        glassBorder = SonaraGlassBorder,
        playerSurface = SonaraPlayerSurface,
        textPrimary = SonaraOnBackground,
        textSecondary = SonaraOnSurfaceMuted,
        textMuted = SonaraOnSurfaceMuted,
    )
}

/** Convenience accessor for the extended semantic roles. */
@Composable
fun sonaraColors(): SonaraColors = LocalSonaraColors.current

@Composable
fun SonaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonaraColorScheme,
        typography = SonaraTypography,
        content = content,
    )
}
