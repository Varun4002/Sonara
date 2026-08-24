package com.sonara.ui

import androidx.compose.ui.unit.Dp
import com.google.common.truth.Truth.assertThat
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.SonaraGlassBorder
import com.sonara.ui.theme.SonaraGlassSurface
import com.sonara.ui.theme.SonaraColorScheme
import org.junit.Test

class DesignTokensTest {

    @Test
    fun `spacing scale is strictly increasing`() {
        val scale = listOf(
            SonaraSpacing.xxs,
            SonaraSpacing.xs,
            SonaraSpacing.sm,
            SonaraSpacing.md,
            SonaraSpacing.lg,
            SonaraSpacing.xl,
            SonaraSpacing.xxl,
            SonaraSpacing.xxxl,
            SonaraSpacing.huge,
            SonaraSpacing.massive,
        )
        assertThat(scale).isInOrder(Comparator<Dp> { a, b -> a.value.compareTo(b.value) })
        assertThat(scale.distinct()).hasSize(scale.size)
    }

    @Test
    fun `semantic spacing roles stay within the scale`() {
        val scale = setOf(
            SonaraSpacing.xxs, SonaraSpacing.xs, SonaraSpacing.sm,
            SonaraSpacing.md, SonaraSpacing.lg, SonaraSpacing.xl,
            SonaraSpacing.xxl, SonaraSpacing.xxxl, SonaraSpacing.huge,
            SonaraSpacing.massive,
        )
        assertThat(SonaraSpacing.screenPadding).isIn(scale)
        assertThat(SonaraSpacing.sectionGap).isIn(scale)
    }

    @Test
    fun `nav bar height is usable`() {
        assertThat(SonaraSpacing.navBarHeight.value).isAtLeast(48f)
    }

    @Test
    fun `motion durations are ordered fast to ambient`() {
        assertThat(SonaraMotion.Fast).isLessThan(SonaraMotion.Normal)
        assertThat(SonaraMotion.Normal).isLessThan(SonaraMotion.PageTransition)
        assertThat(SonaraMotion.PageTransition).isLessThan(SonaraMotion.PlayerExpand)
        assertThat(SonaraMotion.PlayerExpand).isLessThan(SonaraMotion.Ambient)
        assertThat(SonaraMotion.Ambient).isLessThan(2000)
    }

    @Test
    fun `theme is always dark`() {
        val background = SonaraColorScheme.background
        // Luminance below 0.2 keeps the ambient environment dark.
        assertThat(background.luminanceCompat()).isLessThan(0.2f)
    }

    @Test
    fun `glass border is translucent`() {
        assertThat(SonaraGlassBorder.alpha).isEqualTo(0x24 / 255f)
        assertThat(SonaraGlassSurface.alpha).isEqualTo(0x14 / 255f)
    }
}

// Simple luminance estimate without pulling android graphics.
private fun androidx.compose.ui.graphics.Color.luminanceCompat(): Float =
    (0.2126f * red + 0.7152f * green + 0.0722f * blue)
