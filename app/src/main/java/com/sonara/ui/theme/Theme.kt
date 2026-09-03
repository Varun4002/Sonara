package com.sonara.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sonara.ambient.AmbientPalette
import com.sonara.ambient.colorFromHsl
import com.sonara.ambient.colorToHsl

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
    val elevatedSurface: Color = SonaraElevatedSurface,
    val glassSurface: Color = SonaraGlassSurface,
    val glassBorder: Color = SonaraGlassBorder,
    val playerSurface: Color = SonaraPlayerSurface,
    val textPrimary: Color = SonaraOnBackground,
    val textSecondary: Color = SonaraOnSurfaceMuted,
    val textMuted: Color = SonaraOnSurfaceMuted,
)

val LocalSonaraColors = staticCompositionLocalOf { SonaraColors() }

/** Translucent glass roles derived from the active ambient palette. */
@Immutable
data class GlassTint(
    val surface: Color,
    val border: Color,
)

/**
 * Maps the current song's palette onto the liquid-glass roles. The glass stays
 * mostly neutral — a whisper of the environment's hue at low alpha, a slightly
 * brighter rim — so navigation and mini-player inherit the room's lighting
 * without becoming colored surfaces themselves.
 */
fun AmbientPalette.glassTint(): GlassTint {
    val (hue, saturation, _) = colorToHsl(primary)
    return GlassTint(
        surface = colorFromHsl(hue, saturation * 0.45f, l = 0.62f, alpha = 0.10f),
        border = colorFromHsl(hue, saturation * 0.50f, l = 0.78f, alpha = 0.24f),
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
