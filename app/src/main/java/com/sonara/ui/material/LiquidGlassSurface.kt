package com.sonara.ui.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ambient.AmbientVisualState

/**
 * The liquid-glass material as a composable. Every glass surface in Sonara —
 * navigation, mini-player, controls — renders through this; differences come
 * from [intensity], shape and content, never from separate rendering logic.
 * Engine and tint come from [LocalAmbientEngine]/[LocalGlassTint], provided
 * by the shell; without a provider the surface degrades to static glass.
 *
 * The backdrop is sampled analytically from the shared ambient field
 * geometry at the panel's window position, so the material genuinely
 * refracts the environment behind it. On API < 33 the renderer falls back
 * to a canvas approximation of the same optics.
 *
 * [lens] (local px, animated by the caller) renders the selected-item
 * glass-in-glass. [cornerRadius] must match [shape] — the material's SDF
 * needs the radius.
 */
@Composable
fun LiquidGlassSurface(
    shape: Shape,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    intensity: LiquidGlassTokens = LiquidGlassTokens.Standard,
    lens: LensRect? = null,
    accentColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val engine = LocalAmbientEngine.current
    val tint = LocalGlassTint.current
    val windowInfo = LocalWindowInfo.current
    var visual by remember { mutableStateOf(AmbientVisualState()) }
    var frameTimeMs by remember { mutableStateOf(0f) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val shader = remember { if (isAgslAvailable) newLiquidGlassShader() else null }
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    val tokens = if (engine == null) {
        LiquidGlassTokens.Static
    } else {
        LiquidGlassTokens.forMode(visual.mode, intensity)
    }

    LaunchedEffect(engine) {
        engine?.state?.collect { visual = it }
    }
    // Deterministic frame clock — identical across every glass instance, so
    // all surfaces and the background render the same environment frame.
    LaunchedEffect(Unit) {
        var epochNanos = -1L
        while (true) {
            withFrameNanos { now ->
                if (epochNanos < 0L) epochNanos = now
                frameTimeMs = (now - epochNanos) / 1_000_000f
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { rootOffset = it.positionInRoot() },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLiquidGlass(
                shader = shader,
                visual = visual,
                timeMs = frameTimeMs,
                origin = rootOffset,
                fieldSpaceSize = androidx.compose.ui.geometry.Size(
                    windowInfo.containerSize.width.toFloat(),
                    windowInfo.containerSize.height.toFloat(),
                ),
                cornerRadiusPx = cornerRadiusPx,
                tokens = tokens,
                tint = tint,
                lens = lens,
                accentColor = accentColor,
            )
        }
        content()
    }
}
