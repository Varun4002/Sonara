package com.sonara.ui.material

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * AGSL source for the liquid-glass material. The shader analytically
 * re-evaluates the shared ambient field geometry ([com.sonara.ambient.AmbientFields]
 * — same anchors, drifts, clock) at refracted, per-channel-displaced sample
 * positions, so the glass genuinely samples the environment behind it rather
 * than overlaying an approximation of it.
 *
 * Pipeline: refraction displacement (edge-weighted, lens-aware) → chromatic
 * channel separation → multi-tap blur → luminance-aligned neutral frost →
 * ambient tint contamination → specular top rim → inner edge lift → lower
 * shade → selected-item lens (brighter glass-in-glass with its own rim).
 */
const val LiquidGlassShaderSource: String = """
uniform float2 uOrigin;       // glass top-left in window field space
uniform float2 uSize;         // glass size in px (local)
uniform float uCornerRadius;  // px
uniform float2 uFieldSpace;   // window field space size in px

uniform float3 uBase;
uniform float3 uField0;
uniform float3 uField1;
uniform float3 uField2;
uniform float3 uField3;
uniform float4 uFieldAlpha;   // per-field draw alpha; 0 disables the field

uniform float uTime;
uniform float uDrift;
uniform float uPulse;

uniform float uRefraction;
uniform float uRefractionFalloff;
uniform float uChromatic;
uniform float uBlurSpread;
uniform float uFrost;
uniform float3 uTint;
uniform float uTintAlpha;
uniform float uSpecular;
uniform float uEdgeHighlight;
uniform float uLowerShade;

uniform float4 uLens;         // center.xy, halfSize.xy in local px; z <= 0 disables
uniform float uLensRadius;
uniform float3 uLensColor;    // accent-derived color for the selected item

float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float2 rectNormal(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - halfSize + r;
    float2 aq = max(q, 0.0);
    float sx = p.x < 0.0 ? -1.0 : 1.0;
    float sy = p.y < 0.0 ? -1.0 : 1.0;
    return normalize(float2(sx, sy) * aq + float2(1e-5, 1e-5));
}

float2 fieldCenter(float ax, float ay, float dx, float dy, float fx, float fy, float ph) {
    return float2(
        uFieldSpace.x * (ax + dx * uDrift * sin(uTime * fx + ph)),
        uFieldSpace.y * (ay + dy * uDrift * sin(uTime * fy + ph * 1.7))
    );
}

float3 blendField(float3 col, float2 c, float radiusFrac, float3 color, float alpha, float2 win) {
    float r = uFieldSpace.x * radiusFrac * uPulse;
    float f = clamp(1.0 - distance(win, c) / r, 0.0, 1.0);
    return mix(col, color, alpha * f);
}

float3 envColor(float2 win) {
    float3 col = uBase;
    float2 c0 = fieldCenter(0.30, 0.22, 0.22, 0.16, 0.11, 0.07, 0.0);
    col = blendField(col, c0, 0.85, uField0, uFieldAlpha.x, win);
    float2 c1 = fieldCenter(0.75, 0.45, 0.18, 0.20, 0.09, 0.13, 2.2);
    col = blendField(col, c1, 0.75, uField1, uFieldAlpha.y, win);
    float2 c2 = fieldCenter(0.55, 0.85, 0.25, 0.12, 0.07, 0.10, 4.1);
    col = blendField(col, c2, 0.45, uField2, uFieldAlpha.z, win);
    float2 c3 = fieldCenter(0.15, 0.75, 0.15, 0.18, 0.13, 0.08, 5.6);
    col = blendField(col, c3, 0.30, uField3, uFieldAlpha.w, win);
    return col;
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord;
    float2 halfSize = uSize * 0.5;
    float2 pc = p - halfSize;
    float sd = sdRoundRect(pc, halfSize, uCornerRadius);
    float alpha = 1.0 - smoothstep(-1.0, 0.5, sd);
    if (alpha <= 0.0) {
        return half4(0.0);
    }

    // Refraction: displacement bends inward, strongest at the curved edge.
    float edgeT = clamp(1.0 + sd / uRefractionFalloff, 0.0, 1.0);
    float2 disp = -rectNormal(pc, halfSize, uCornerRadius) * (uRefraction * edgeT * edgeT);

    // The selected-item lens is glass inside glass: it bends the backdrop too.
    float sdLens = 1e9;
    if (uLens.z > 0.0) {
        float2 pl = pc - uLens.xy;
        sdLens = sdRoundRect(pl, uLens.zw, uLensRadius);
        float edgeL = clamp(1.0 + sdLens / uRefractionFalloff, 0.0, 1.0);
        disp += -rectNormal(pl, uLens.zw, uLensRadius) * (uRefraction * 0.7 * edgeL * edgeL);
    }

    float2 win = fragCoord + uOrigin;
    float3 col;
    if (uBlurSpread <= 0.0 && uRefraction <= 0.0) {
        col = envColor(win);
    } else {
        // Chromatic response: channels sample at slightly different
        // displacement strengths, then recombine. Kept subtle by tokens.
        float3 acc = float3(0.0, 0.0, 0.0);
        float2 o1 = float2(0.9, 0.5) * uBlurSpread;
        float2 o2 = float2(-0.7, -0.8) * uBlurSpread;
        float2 base = win + disp;
        acc.r = (envColor(base).r + envColor(base + o1 - disp * uChromatic).r +
                 envColor(base + o2 - disp * uChromatic).r) / 3.0;
        acc.g = (envColor(base).g + envColor(base + o1).g +
                 envColor(base + o2).g) / 3.0;
        acc.b = (envColor(base).b + envColor(base + o1 + disp * uChromatic).b +
                 envColor(base + o2 + disp * uChromatic).b) / 3.0;
        col = acc;
    }

    // Frost: luminance-aligned neutral so dark environments keep dark glass.
    float lum = dot(col, float3(0.299, 0.587, 0.114));
    float3 neutral = float3(mix(lum, 0.62, 0.6));
    col = mix(col, neutral, uFrost);
    col = mix(col, uTint, uTintAlpha);

    // Specular: top-weighted rim light; corners read slightly brighter.
    float edge = clamp(1.0 + sd / 1.5, 0.0, 1.0);
    float topW = clamp(1.0 - p.y / uSize.y, 0.0, 1.0);
    col += float3(1.0, 1.0, 1.0) * uSpecular * edge * (0.2 + 0.8 * topW);
    float inner = clamp(1.0 + sd / 6.0, 0.0, 1.0);
    col += float3(1.0, 1.0, 1.0) * uEdgeHighlight * inner * (0.3 + 0.4 * topW);
    float bottomW = clamp((p.y - uSize.y * 0.6) / (uSize.y * 0.4), 0.0, 1.0);
    col *= 1.0 - uLowerShade * bottomW * clamp(1.0 + sd / 8.0, 0.0, 1.0);

    // Lens: glossy, bright, white-glow selected-item glass-in-glass.
    if (uLens.z > 0.0) {
        float inside = clamp(-sdLens, 0.0, 1.0);
        float softEdge = smoothstep(0.0, 3.0, inside);
        // Contact shadow: dark band outside the lens for optical separation.
        float shadow = smoothstep(5.0, 0.5, max(sdLens, 0.0));
        col *= 1.0 - 0.30 * shadow;
        // White tonal fill: glossy bright interior, not colored.
        col = mix(col, float3(1.0, 1.0, 1.0), 0.22 * softEdge);
        // Brightness lift: the lens is noticeably brighter than surrounding glass.
        col *= 1.0 + 0.30 * softEdge;
        // Frost reduction inside the lens: clearer glass = more glossy.
        float3 lensBase = col / max(uFrost + 0.01, 0.01);
        col = mix(col, lensBase, 0.20 * softEdge);
        // Wet specular: sharp bright highlight across the top half of the lens.
        float rimL = clamp(1.0 + sdLens / 0.8, 0.0, 1.0);
        float lensTop = uLens.y - uLens.w;
        float topL = clamp(1.0 - (p.y - lensTop) / max(uLens.w * 2.0, 1.0), 0.0, 1.0);
        col += float3(1.0, 1.0, 1.0) * uSpecular * 1.5 * rimL * (0.1 + 0.9 * topL);
        // Glossy hot-spot: concentrated white highlight near the top-center.
        float2 lensCenter = uLens.xy;
        float2 toCenter = p - (uLens.xy + float2(0.0, -uLens.w * 0.3));
        float hotspot = exp(-dot(toCenter, toCenter) / (uLens.w * uLens.w * 0.6));
        col += float3(1.0, 1.0, 1.0) * 0.18 * hotspot * softEdge;
        // White inner glow radiating from the edge inward.
        col += float3(1.0, 1.0, 1.0) * 0.15 * softEdge * rimL;
    }

    return half4(col, alpha);
}
"""

/** RuntimeShader is an API 33+ capability; callers must fall back below that. */
@RequiresApi(33)
fun newLiquidGlassShader(): RuntimeShader = RuntimeShader(LiquidGlassShaderSource)

val isAgslAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= 33
