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
import com.sonara.ambient.AmbientFields
import com.sonara.ambient.AmbientVisualMode
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ambient.AmbientVisualState
import com.sonara.ambient.center
import com.sonara.ambient.fieldColors

/**
 * The Sonara liquid environment: a near-black tonal field with a few large,
 * slowly drifting color fields on top. Palette and energy come from the
 * ambient engine; all state is read during the draw phase only, so engine
 * ticks never recompose the UI tree.
 *
 * Time is derived deterministically from the frame clock (not accumulated),
 * so every glass surface and background instance renders identical frames —
 * which is what lets the liquid-glass material sample a synced copy of the
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
 * The shared field renderer, driven by the shared [com.sonara.ambient.AmbientFields]
 * geometry so the AGSL glass shader samples the identical environment.
 * [timeMs] is the shared ambient clock; no per-frame allocation beyond the
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

    val colors = palette.fieldColors()
    AmbientFields.take(fieldCount).forEachIndexed { index, spec ->
        val (color, fieldAlpha) = colors[index]
        val (cx, cy) = spec.center(t, drift, w, h)
        val px = originX + cx
        val py = originY + cy
        val radius = w * spec.radiusFraction * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = color.alpha * fieldAlpha * modeAlpha),
                    Color.Transparent,
                ),
                center = Offset(px, py),
                radius = radius,
            ),
            radius = radius,
            center = Offset(px, py),
        )
    }
}
