package com.sonara.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.sonara.ui.designsystem.SonaraMotion

/**
 * Press feedback for artwork-led cards: small scale-down on press, smooth
 * recovery on release. No bounce, no ripple — the environment stays calm.
 */
fun Modifier.sonaraPressScale(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(SonaraMotion.Normal, easing = SonaraMotion.StandardEasing),
        label = "sonara-press-scale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
