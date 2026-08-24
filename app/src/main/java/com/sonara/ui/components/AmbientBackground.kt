package com.sonara.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sonara.ambient.AmbientVisualMode
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ambient.AmbientVisualState
import kotlin.math.sin

/**
 * The Sonara liquid environment: a near-black tonal field with a few large,
 * slowly drifting color fields on top. Palette and energy come from the
 * ambient engine; all state is read during the draw phase only, so engine
 * ticks never recompose the UI tree.
 *
 * Time is derived deterministically from the frame clock (not accumulated),
 * so every [AmbientBackground] instance on screen renders identical frames —
 * which is what lets [SonaraLiquidGlass] show a synced, blurred copy of the
 * environment as its backdrop.
 */
@Composable
fun AmbientBackground(
    engine: AmbientVisualEngine,
    modifier: Modifier = Modifier,
) {
    var visual by remember { mutableStateOf(AmbientVisualState()) }
    var frameTimeMs by remember { mutableStateOf(0f) }

    LaunchedEffect(engine) {
        engine.state.collect { visual = it }
    }
    LaunchedEffect(Unit) {
        var epochNanos = -1L
        while (true) {
            withFrameNanos { now ->
                if (epochNanos < 0L) epochNanos = now
                frameTimeMs = (now - epochNanos) / 1_000_000f
            }
        }
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = "Ambient background" },
    ) {
        drawRect(palette(visual).base)
        drawAmbientFields(visual, frameTimeMs)
    }
}

internal fun palette(visual: AmbientVisualState) = visual.resolved()

/**
 * The shared field renderer. [timeMs] is the shared ambient clock; motion is
 * layered sines — organic drift with no per-frame allocation beyond the
 * gradient shader itself.
 *
 * [originX]/[originY] position the field geometry in the full-environment
 * coordinate space, and [fieldWidth]/[fieldHeight] give that space's size —
 * a panel drawing with its own root offset and the window size renders
 * exactly the slice of environment that sits behind it.
 */
internal fun DrawScope.drawAmbientFields(
    visual: AmbientVisualState,
    timeMs: Float,
    alphaScale: Float = 1f,
    originX: Float = 0f,
    originY: Float = 0f,
    fieldWidth: Float = size.width,
    fieldHeight: Float = size.height,
) {
    val palette = visual.resolved()
    val energy = visual.energy
    val w = fieldWidth
    val h = fieldHeight
    val t = timeMs / 1000f

    val fieldCount = when (visual.mode) {
        AmbientVisualMode.Off -> 0
        AmbientVisualMode.Reduced -> 2
        AmbientVisualMode.On -> 4
    }
    val modeAlpha = when (visual.mode) {
        AmbientVisualMode.Reduced -> 0.6f
        else -> 1f
    } * alphaScale
    val pulse = 1f + energy.beat * 0.05f
    // Bass/overall energy widens the drift displacement — large-scale liquid
    // movement reacts to the music while the clock stays deterministic.
    val drift = 0.8f + energy.overall * 0.5f

    fun drawField(
        color: Color,
        anchorX: Float, anchorY: Float,
        driftX: Float, driftY: Float,
        fx: Float, fy: Float, phase: Float,
        radiusFraction: Float,
        alpha: Float,
    ) {
        val cx = originX + w * (anchorX + driftX * drift * sin(t * fx + phase))
        val cy = originY + h * (anchorY + driftY * drift * sin(t * fy + phase * 1.7f))
        val radius = w * radiusFraction * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = color.alpha * alpha * modeAlpha),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius,
            ),
            radius = radius,
            center = Offset(cx, cy),
        )
    }

    if (fieldCount > 0) drawField(palette.primary, 0.30f, 0.22f, 0.22f, 0.16f, 0.11f, 0.07f, 0f, 0.85f, 0.55f)
    if (fieldCount > 1) drawField(palette.secondary, 0.75f, 0.45f, 0.18f, 0.20f, 0.09f, 0.13f, 2.2f, 0.75f, 0.50f)
    if (fieldCount > 2) drawField(palette.highlight, 0.55f, 0.85f, 0.25f, 0.12f, 0.07f, 0.10f, 4.1f, 0.45f, 0.30f)
    if (fieldCount > 3) drawField(palette.accent, 0.15f, 0.75f, 0.15f, 0.18f, 0.13f, 0.08f, 5.6f, 0.30f, 0.16f)
}
