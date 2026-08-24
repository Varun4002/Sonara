package com.sonara.ambient

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmbientPaletteTest {

    @Test
    fun `hsl round trip preserves color within tolerance`() {
        val original = Color(0xFF8E7BFF)
        val (h, s, l) = colorToHsl(original)
        val restored = colorFromHsl(h, s, l, original.alpha)
        assertThat(restored.red).isWithin(0.01f).of(original.red)
        assertThat(restored.green).isWithin(0.01f).of(original.green)
        assertThat(restored.blue).isWithin(0.01f).of(original.blue)
    }

    @Test
    fun `tonal processing keeps colors dark enough for text contrast`() {
        val blinding = Color(0xFFFFEA00) // harsh bright yellow
        val toned = toneForAmbient(blinding, saturation = 0.55f, lightness = 0.22f)
        val (_, _, lightness) = colorToHsl(toned)
        assertThat(lightness).isAtMost(0.22f)
    }

    @Test
    fun `tonal processing caps oversaturation`() {
        val neon = Color(0xFF00FF3C)
        val toned = toneForAmbient(neon, saturation = 0.55f, lightness = 0.22f)
        val (_, saturation, _) = colorToHsl(toned)
        // HSL round-trip has float error; allow a small epsilon.
        assertThat(saturation).isLessThan(0.56f)
    }

    @Test
    fun `empty candidates fall back to neutral`() {
        assertThat(buildAmbientPalette(emptyList())).isEqualTo(AmbientPalette.Neutral)
    }

    @Test
    fun `base field stays near black regardless of artwork`() {
        val candidates = listOf(Color(0xFFFF7043), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFF4FC3F7))
        val palette = buildAmbientPalette(candidates)
        val (_, _, baseLightness) = colorToHsl(palette.base)
        assertThat(baseLightness).isLessThan(0.10f)
    }

    @Test
    fun `generated palettes are deterministic per track`() {
        val a1 = generatedAmbientPalette("aurora")
        val a2 = generatedAmbientPalette("aurora")
        val d = generatedAmbientPalette("drift")
        assertThat(a1).isEqualTo(a2)
        assertThat(a1).isNotEqualTo(d)
    }

    @Test
    fun `different tracks produce visibly different environments`() {
        val palettes = DemoIds.all.map { generatedAmbientPalette(it) }
        // Primary field hues must differ meaningfully between tracks.
        val hues = palettes.map { colorToHsl(it.primary).first }
        hues.forEachIndexed { i, hue ->
            hues.forEachIndexed { j, other ->
                if (i < j) {
                    val diff = kotlin.math.abs(hue - other)
                    val separation = minOf(diff, 360f - diff)
                    assertThat(separation).isGreaterThan(20f)
                }
            }
        }
    }

    @Test
    fun `the app never becomes permanently one color`() {
        // Across the demo catalog, palettes must span more than one hue family.
        val hues = DemoIds.all.map { colorToHsl(generatedAmbientPalette(it).primary).first }.distinct()
        assertThat(hues.size).isAtLeast(3)
    }

    private object DemoIds {
        val all = listOf("aurora", "drift", "halo", "lumen", "vela")
    }
}
