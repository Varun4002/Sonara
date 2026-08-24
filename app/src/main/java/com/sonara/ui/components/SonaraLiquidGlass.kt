package com.sonara.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ambient.AmbientVisualState
import com.sonara.ui.designsystem.SonaraBlur

/**
 * Sonara's liquid-glass surface, modeled on the iOS 26 material recipe:
 *
 * 1. a blurred live copy of the ambient environment as backdrop — drawn in
 *    the panel's root position against the window-sized field geometry, so it
 *    genuinely matches the environment behind it (GPU blur on API 31+; the
 *    soft radial gradients alone read as frost below that),
 * 2. a diagonal translucent white fill (stronger top-leading, fainter
 *    bottom-trailing) so the surface has transparency variation,
 * 3. a 1px edge that is brighter along the top — the specular highlight of
 *    light catching the glass rim.
 *
 * The backdrop comes from the same deterministic ambient clock as
 * [AmbientBackground]: same palette, same field positions, same time, same
 * energy.
 */
@Composable
fun SonaraLiquidGlass(
    engine: AmbientVisualEngine,
    shape: Shape,
    modifier: Modifier = Modifier,
    backdropBlur: Modifier = Modifier.blur(SonaraBlur.glass),
    content: @Composable () -> Unit,
) {
    var visual by remember { mutableStateOf(AmbientVisualState()) }
    var frameTimeMs by remember { mutableStateOf(0f) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val windowInfo = LocalWindowInfo.current

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

    Box(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { rootOffset = it.positionInRoot() },
    ) {
        // Frosted backdrop: the actual slice of environment behind this panel.
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .then(backdropBlur),
        ) {
            drawAmbientFields(
                visual = visual,
                timeMs = frameTimeMs,
                alphaScale = 1.4f,
                originX = rootOffset.x,
                originY = rootOffset.y,
                fieldWidth = windowInfo.containerSize.width.toFloat(),
                fieldHeight = windowInfo.containerSize.height.toFloat(),
            )
        }
        // Diagonal translucent fill — transparency variation, not a flat tint.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                ),
        )
        content()
        // Edge definition + specular top rim, drawn last so it stays crisp.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = shape,
                ),
        )
    }
}
