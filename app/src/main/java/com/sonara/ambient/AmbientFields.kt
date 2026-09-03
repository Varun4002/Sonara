package com.sonara.ambient

import androidx.compose.ui.graphics.Color

/**
 * The ambient field geometry, shared by the Canvas renderer and the AGSL
 * glass shader so both paint the identical environment. Field motion:
 * center oscillates on layered sines; radius scales with beat energy.
 */
data class AmbientFieldSpec(
    val anchorX: Float,
    val anchorY: Float,
    val driftX: Float,
    val driftY: Float,
    val freqX: Float,
    val freqY: Float,
    val phase: Float,
    val radiusFraction: Float,
    val alpha: Float,
)

internal val AmbientFields = listOf(
    AmbientFieldSpec(0.30f, 0.22f, 0.22f, 0.16f, 0.11f, 0.07f, 0.0f, 0.85f, 0.85f),
    AmbientFieldSpec(0.75f, 0.45f, 0.18f, 0.20f, 0.09f, 0.13f, 2.2f, 0.75f, 0.75f),
    AmbientFieldSpec(0.55f, 0.85f, 0.25f, 0.12f, 0.07f, 0.10f, 4.1f, 0.45f, 0.55f),
    AmbientFieldSpec(0.15f, 0.75f, 0.15f, 0.18f, 0.13f, 0.08f, 5.6f, 0.30f, 0.40f),
)

/** Field center in window pixels for [spec] at [timeS], matching the canvas math. */
internal fun AmbientFieldSpec.center(timeS: Float, drift: Float, w: Float, h: Float): Pair<Float, Float> {
    val cx = w * (anchorX + driftX * drift * kotlin.math.sin(timeS * freqX + phase))
    val cy = h * (anchorY + driftY * drift * kotlin.math.sin(timeS * freqY + phase * 1.7f))
    return cx to cy
}

/** Field colors in palette order, with their draw alphas. */
internal fun AmbientPalette.fieldColors(): List<Pair<Color, Float>> = listOf(
    primary to AmbientFields[0].alpha,
    secondary to AmbientFields[1].alpha,
    highlight to AmbientFields[2].alpha,
    accent to AmbientFields[3].alpha,
)
