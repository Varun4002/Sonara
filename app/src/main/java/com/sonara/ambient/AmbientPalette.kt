package com.sonara.ambient

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The tonally processed color set the whole environment is painted from.
 * Derived from the current song's artwork where available; never a raw
 * dominant color — every role is darkened/desaturated for a near-black
 * ambient field with readable contrast.
 */
data class AmbientPalette(
    /** Deep background tone the liquid fields float on. Always near-black. */
    val base: Color,
    /** Large liquid field color — the environment's dominant presence. */
    val primary: Color,
    /** Counter-field color; hue-shifted from [primary] for depth. */
    val secondary: Color,
    /** Small bright field / artwork glow. Restrained. */
    val highlight: Color,
    /** Sparse accent for selection states and the progress line. */
    val accent: Color,
) {
    companion object {
        /** Calm neutral environment used before any track and on failure. */
        val Neutral = AmbientPalette(
            base = Color(0xFF0B0B0F),
            primary = Color(0xFF1A1D2E),
            secondary = Color(0xFF14202A),
            highlight = Color(0xFF232A3E),
            accent = Color(0xFF5E6B85),
        )
    }
}

/** HSL components of an ARGB [color], h in 0..360, s/l in 0..1. */
fun colorToHsl(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val l = (maxC + minC) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
    val h = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Triple(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

/** ARGB color from HSL components, keeping [color]'s alpha. */
fun colorFromHsl(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
    val hn = ((h % 360f) + 360f) % 360f / 60f
    val c = (1f - abs(2f * l - 1f)) * s.coerceIn(0f, 1f)
    val x = c * (1f - abs(hn % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when (hn.roundToInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

/**
 * Restrains any candidate artwork color into a usable ambient role:
 * oversaturated colors are calmed, bright colors are pulled down so text and
 * glass stay readable on top. [lightness] pins the target luminance band.
 */
fun toneForAmbient(color: Color, saturation: Float, lightness: Float): Color {
    val (h, s, _) = colorToHsl(color)
    return colorFromHsl(
        h = h,
        s = s.coerceIn(0.18f, saturation),
        l = lightness,
    )
}

/**
 * Builds a full palette from raw artwork candidate colors (dominant + vibrant
 * samples, any order). Deterministic: same inputs, same palette. Falls back
 * toward neutral when too few candidates exist.
 */
fun buildAmbientPalette(candidates: List<Color>): AmbientPalette {
    if (candidates.isEmpty()) return AmbientPalette.Neutral
    val primary = toneForAmbient(candidates[0], saturation = 0.55f, lightness = 0.22f)
    // Counter-field sits 30-60 degrees away so fields separate visually.
    val secondaryHue = colorToHsl(primary).first + 40f
    val secondary = candidates.getOrNull(1)
        ?.let { toneForAmbient(it, saturation = 0.45f, lightness = 0.16f) }
        ?: colorFromHsl(secondaryHue, 0.40f, 0.16f)
    val highlight = candidates.getOrNull(2)
        ?.let { toneForAmbient(it, saturation = 0.50f, lightness = 0.30f) }
        ?: colorFromHsl(secondaryHue + 20f, 0.45f, 0.30f)
    val accent = candidates.getOrNull(3)
        ?.let { toneForAmbient(it, saturation = 0.65f, lightness = 0.52f) }
        ?: colorFromHsl(colorToHsl(primary).first, 0.55f, 0.52f)
    val (baseHue, baseSat, _) = colorToHsl(primary)
    val base = colorFromHsl(baseHue, baseSat * 0.6f, 0.045f)
    return AmbientPalette(
        base = base,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        accent = accent,
    )
}

/**
 * Deterministic fallback palette for tracks without artwork. Seeds from the
 * stable media id so a given song always wears the same environment, while
 * different songs land in different hue families. The hue is the scrambled
 * hash mapped continuously around the wheel — the app never becomes
 * permanently any single color.
 */
fun generatedAmbientPalette(mediaId: String): AmbientPalette {
    val fraction =
        (scramble(mediaId.hashCode()).toLong() and 0xffffffffL).toFloat() / 4_294_967_296f
    val primary = colorFromHsl(fraction * 360f, 0.42f, 0.22f)
    return buildAmbientPalette(listOf(primary))
}

/** SplitMix-style finalizer: spreads similar ids across slots evenly. */
private fun scramble(seed: Int): Int {
    var z = seed
    z = (z xor (z ushr 16)) * 0x7feb352d.toInt()
    z = (z xor (z ushr 15)) * 0x846ca68b.toInt()
    return z xor (z ushr 16)
}
