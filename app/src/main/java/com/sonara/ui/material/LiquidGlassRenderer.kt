package com.sonara.ui.material

import android.graphics.Paint
import android.graphics.RuntimeShader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.sonara.ambient.AmbientVisualMode
import com.sonara.ambient.AmbientVisualState
import com.sonara.ambient.AmbientFields
import com.sonara.ambient.center
import com.sonara.ambient.fieldColors
import com.sonara.ui.theme.GlassTint

/**
 * Draw-phase renderer for the liquid-glass material. One implementation for
 * every glass surface; differences come from tokens and geometry only.
 *
 * API 33+ runs the AGSL shader (refraction, chromatic response, analytic
 * environment sampling). Older devices use the canvas fallback: the same
 * shared field renderer draws the backdrop slice, frost and gradient
 * highlights approximate the optics without RuntimeShader.
 */
fun DrawScope.drawLiquidGlass(
    shader: RuntimeShader?,
    visual: AmbientVisualState,
    timeMs: Float,
    origin: Offset,
    fieldSpaceSize: androidx.compose.ui.geometry.Size,
    cornerRadiusPx: Float,
    tokens: LiquidGlassTokens,
    tint: GlassTint,
    lens: LensRect? = null,
    accentColor: Color = Color.Transparent,
) {
    if (shader != null) {
        drawShaderGlass(shader, visual, timeMs, origin, fieldSpaceSize, cornerRadiusPx, tokens, tint, lens, accentColor)
    } else {
        drawFallbackGlass(visual, timeMs, origin, fieldSpaceSize, cornerRadiusPx, tokens, tint, lens, accentColor)
    }
}

/** Selected-item lens rectangle in local px (center + half size). */
data class LensRect(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float,
)

@Suppress("NewApi")
private fun DrawScope.drawShaderGlass(
    shader: RuntimeShader,
    visual: AmbientVisualState,
    timeMs: Float,
    origin: Offset,
    fieldSpaceSize: androidx.compose.ui.geometry.Size,
    cornerRadiusPx: Float,
    tokens: LiquidGlassTokens,
    tint: GlassTint,
    lens: LensRect?,
    accentColor: Color,
) {
    val palette = visual.resolved()
    val energy = visual.energy
    val fieldAlphas = when (visual.mode) {
        AmbientVisualMode.Off -> floatArrayOf(0f, 0f, 0f, 0f)
        AmbientVisualMode.Reduced -> floatArrayOf(0.6f, 0.6f, 0f, 0f)
        AmbientVisualMode.On -> floatArrayOf(1f, 1f, 1f, 1f)
    }
    val colors = palette.fieldColors()

    shader.apply {
        setFloatUniform("uOrigin", origin.x, origin.y)
        setFloatUniform("uSize", size.width, size.height)
        setFloatUniform("uCornerRadius", cornerRadiusPx)
        setFloatUniform("uFieldSpace", fieldSpaceSize.width, fieldSpaceSize.height)
        setFloatUniform("uBase", palette.base.red, palette.base.green, palette.base.blue)
        colors.forEachIndexed { index, (color, _) ->
            setFloatUniform("uField$index", color.red, color.green, color.blue)
        }
        setFloatUniform(
            "uFieldAlpha",
            fieldAlphas[0], fieldAlphas[1], fieldAlphas[2], fieldAlphas[3],
        )
        setFloatUniform("uTime", timeMs / 1000f)
        setFloatUniform("uDrift", 0.8f + energy.overall * 0.5f)
        setFloatUniform("uPulse", 1f + energy.beat * 0.05f)
        setFloatUniform("uRefraction", tokens.refractionPx)
        setFloatUniform("uRefractionFalloff", tokens.refractionFalloffPx)
        setFloatUniform("uChromatic", tokens.chromaticPx)
        setFloatUniform("uBlurSpread", tokens.blurSpreadPx)
        setFloatUniform("uFrost", tokens.frost)
        setFloatUniform("uTint", tint.surface.red, tint.surface.green, tint.surface.blue)
        setFloatUniform("uTintAlpha", tint.surface.alpha * tokens.ambientInfluence * 4f)
        // Beat nudges the specular almost imperceptibly — felt, not seen.
        setFloatUniform("uSpecular", tokens.specular * (1f + energy.beat * 0.15f))
        setFloatUniform("uEdgeHighlight", tokens.edgeHighlight)
        setFloatUniform("uLowerShade", tokens.lowerShade)
        if (lens != null) {
            setFloatUniform("uLens", lens.centerX, lens.centerY, lens.halfWidth, lens.halfHeight)
            setFloatUniform("uLensRadius", lens.cornerRadius)
            setFloatUniform("uLensColor", accentColor.red, accentColor.green, accentColor.blue)
        } else {
            setFloatUniform("uLens", 0f, 0f, 0f, 0f)
            setFloatUniform("uLensRadius", 0f)
            setFloatUniform("uLensColor", 0f, 0f, 0f)
        }
    }

    val paint = Paint()
    paint.shader = shader
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

private fun DrawScope.drawFallbackGlass(
    visual: AmbientVisualState,
    timeMs: Float,
    origin: Offset,
    fieldSpaceSize: androidx.compose.ui.geometry.Size,
    cornerRadiusPx: Float,
    tokens: LiquidGlassTokens,
    tint: GlassTint,
    lens: LensRect?,
    accentColor: Color,
) {
    // Same environment, same clock — just sampled without the shader's
    // refraction. Frost and gradient lighting approximate the optics.
    if (visual.mode != AmbientVisualMode.Off) {
        drawAmbientSlice(visual, timeMs, origin, fieldSpaceSize)
    }
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = tokens.frost * 0.9f),
                Color.White.copy(alpha = tokens.frost * 0.4f),
            ),
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                tint.surface.copy(alpha = tint.surface.alpha * tokens.ambientInfluence * 4f),
                Color.Transparent,
                tint.surface.copy(alpha = tint.surface.alpha * tokens.ambientInfluence * 2f),
            ),
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = tokens.specular),
                Color.Transparent,
                Color.Black.copy(alpha = tokens.lowerShade),
            ),
            startY = 0f,
            endY = size.height,
        ),
    )

    // Fallback lens: glossy white-glass selected-item on pre-AGSL.
    if (lens != null) {
        val left = lens.centerX - lens.halfWidth
        val top = lens.centerY - lens.halfHeight
        val right = lens.centerX + lens.halfWidth
        val bottom = lens.centerY + lens.halfHeight
        val corner = androidx.compose.ui.geometry.CornerRadius(lens.cornerRadius, lens.cornerRadius)
        // Contact shadow outside the lens for optical separation.
        val shadowPad = 5f
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = androidx.compose.ui.geometry.Offset(left - shadowPad, top - shadowPad),
            size = androidx.compose.ui.geometry.Size(
                (right - left) + shadowPad * 2,
                (bottom - top) + shadowPad * 2,
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                lens.cornerRadius + shadowPad,
                lens.cornerRadius + shadowPad,
            ),
        )
        // White brightness fill — glossy interior.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = corner,
        )
        // White tonal fill — glossy, not colored.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = corner,
        )
        // Strong specular rim, top-weighted for gloss.
        drawRoundRect(
            color = Color.White.copy(alpha = tokens.specular * 1.0f),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = corner,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
        )
        // Top specular gradient — glossy highlight across the top half.
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.Transparent,
                ),
                startY = top,
                endY = top + (bottom - top) * 0.5f,
            ),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = corner,
        )
    }
}

private fun DrawScope.drawAmbientSlice(
    visual: AmbientVisualState,
    timeMs: Float,
    origin: Offset,
    fieldSpaceSize: androidx.compose.ui.geometry.Size,
) {
    val palette = visual.resolved()
    val t = timeMs / 1000f
    val drift = 0.8f + visual.energy.overall * 0.5f
    val pulse = 1f + visual.energy.beat * 0.05f
    val modeAlpha = if (visual.mode == AmbientVisualMode.Reduced) 0.6f else 1f
    val count = if (visual.mode == AmbientVisualMode.Reduced) 2 else AmbientFields.size
    val colors = palette.fieldColors()

    drawRect(palette.base)
    AmbientFields.take(count).forEachIndexed { index, spec ->
        val (color, fieldAlpha) = colors[index]
        val (cx, cy) = spec.center(t, drift, fieldSpaceSize.width, fieldSpaceSize.height)
        val radius = fieldSpaceSize.width * spec.radiusFraction * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = color.alpha * fieldAlpha * modeAlpha),
                    Color.Transparent,
                ),
                center = Offset(origin.x + cx, origin.y + cy),
                radius = radius,
            ),
            radius = radius,
            center = Offset(origin.x + cx, origin.y + cy),
        )
    }
}
