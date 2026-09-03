package com.sonara.ui.material

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.ambient.AmbientVisualMode

/**
 * Optical parameters of the liquid-glass material. One source of truth for
 * every glass surface; composables never hard-code alphas or radii.
 *
 * Visual budget: refraction and blur dominate, translucency and lighting
 * support, color contamination stays a whisper (5–15%).
 */
data class LiquidGlassTokens(
    /** Neutral frost mixed over the refracted backdrop. */
    val frost: Float,
    /** How strongly the ambient palette contaminates the glass (0..1). */
    val ambientInfluence: Float,
    /** Edge-parallel backdrop displacement, in px at the reference density. */
    val refractionPx: Float,
    /** Falloff distance (px) of edge displacement into the surface. */
    val refractionFalloffPx: Float,
    /** Per-channel displacement spread for chromatic response, in px. */
    val chromaticPx: Float,
    /** Blur tap spread, in px (analytic environment sampling). */
    val blurSpreadPx: Float,
    /** Top-rim specular brightness, 0..1. */
    val specular: Float,
    /** Inner perimeter luminance lift, 0..1. */
    val edgeHighlight: Float,
    /** Darkening toward the bottom inner edge, 0..1. */
    val lowerShade: Float,
    /** Corner radius used by the material's SDF (must match the composable shape). */
    val cornerRadiusPx: Float,
) {
    companion object {
        /** Search fields, secondary chips — quietest glass. */
        val Subtle = LiquidGlassTokens(
            frost = 0.30f,
            ambientInfluence = 0.08f,
            refractionPx = 6f,
            refractionFalloffPx = 40f,
            chromaticPx = 0.6f,
            blurSpreadPx = 7f,
            specular = 0.16f,
            edgeHighlight = 0.05f,
            lowerShade = 0.08f,
            cornerRadiusPx = 0f, // always supplied by the shape at draw time
        )

        /** Navigation, mini-player — the signature surfaces. */
        val Standard = LiquidGlassTokens(
            frost = 0.18f,
            ambientInfluence = 0.18f,
            refractionPx = 10f,
            refractionFalloffPx = 56f,
            chromaticPx = 1.1f,
            blurSpreadPx = 9f,
            specular = 0.30f,
            edgeHighlight = 0.08f,
            lowerShade = 0.12f,
            cornerRadiusPx = 0f,
        )

        /** Now Playing controls and major modal surfaces. */
        val Strong = LiquidGlassTokens(
            frost = 0.18f,
            ambientInfluence = 0.15f,
            refractionPx = 14f,
            refractionFalloffPx = 72f,
            chromaticPx = 1.6f,
            blurSpreadPx = 12f,
            specular = 0.28f,
            edgeHighlight = 0.09f,
            lowerShade = 0.14f,
            cornerRadiusPx = 0f,
        )

        /** Static translucent fallback — ambient Off or pre-AGSL devices. */
        val Static = LiquidGlassTokens(
            frost = 0.34f,
            ambientInfluence = 0.06f,
            refractionPx = 0f,
            refractionFalloffPx = 1f,
            chromaticPx = 0f,
            blurSpreadPx = 0f,
            specular = 0.14f,
            edgeHighlight = 0.05f,
            lowerShade = 0.08f,
            cornerRadiusPx = 0f,
        )

        /** Tokens adjusted for the ambient visual mode. */
        fun forMode(mode: AmbientVisualMode, base: LiquidGlassTokens): LiquidGlassTokens = when (mode) {
            AmbientVisualMode.On -> base
            AmbientVisualMode.Reduced -> base.copy(
                refractionPx = base.refractionPx * 0.5f,
                chromaticPx = 0f,
                ambientInfluence = base.ambientInfluence * 0.7f,
            )
            AmbientVisualMode.Off -> Static
        }
    }
}

/**
 * Dimensional tokens for the floating glass system (navigation, mini-player,
 * their spacing). Screens read these instead of raw dp.
 */
object LiquidGlassDimensions {
    val navigationHeight: Dp = 80.dp
    val navigationRadius: Dp = 34.dp
    val navigationHorizontalMargin: Dp = 16.dp

    val miniPlayerHeight: Dp = 68.dp
    val miniPlayerRadius: Dp = 26.dp
    val miniPlayerArtwork: Dp = 48.dp
    val miniPlayerArtworkRadius: Dp = 13.dp

    /** Vertical gap between mini-player and navigation. */
    val bottomGap: Dp = 10.dp

    /** Space between the navigation and the system gesture area. */
    val navigationBottomInset: Dp = 12.dp

    /** Width of the selected-item lens inset within the navigation. */
    val lensInset: Dp = 8.dp
}
