package com.sonara.ambient

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AmbientVisualEngineTest {

    @Test
    fun `transition interpolates from previous palette to target`() = runTest {
        val engine = AmbientVisualEngine(backgroundScope, transitionMs = 500, tickMs = 100)
        val from = AmbientPalette(base = Color.Black, primary = Color.Red, secondary = Color.Red, highlight = Color.Red, accent = Color.Red)
        val to = AmbientPalette(base = Color.Black, primary = Color.Blue, secondary = Color.Blue, highlight = Color.Blue, accent = Color.Blue)

        engine.transitionTo(from)
        advanceTimeBy(1_000); runCurrent()
        engine.transitionTo(to)
        advanceTimeBy(250); runCurrent()
        val mid = engine.state.value.resolved()
        // Mid-transition: strictly between red and blue.
        assertThat(mid.primary.red).isAtLeast(0.05f)
        assertThat(mid.primary.red).isAtMost(0.95f)
        assertThat(mid.primary.blue).isAtLeast(0.05f)
        assertThat(mid.primary.blue).isAtMost(0.95f)

        advanceTimeBy(1_000); runCurrent()
        assertThat(engine.state.value.transition).isEqualTo(1f)
        assertThat(engine.state.value.resolved().primary).isEqualTo(Color.Blue)
    }

    @Test
    fun `transition to same palette is a no-op`() = runTest {
        val engine = AmbientVisualEngine(this)
        engine.transitionTo(AmbientPalette.Neutral)
        runCurrent()
        assertThat(engine.state.value.transition).isEqualTo(1f)
    }

    @Test
    fun `energy decays gradually after pause instead of freezing`() = runTest {
        val engine = AmbientVisualEngine(backgroundScope, tickMs = 50)
        engine.setPlaying(true)
        advanceTimeBy(10_000); runCurrent()
        val playingEnergy = engine.state.value.energy
        assertThat(playingEnergy.overall).isGreaterThan(0.2f)

        engine.setPlaying(false)
        advanceTimeBy(200); runCurrent()
        val early = engine.state.value.energy.overall
        advanceTimeBy(10_000); runCurrent()
        val settled = engine.state.value.energy.overall
        // Decay is gradual: still visible shortly after, near zero when settled.
        assertThat(early).isGreaterThan(settled)
        assertThat(settled).isLessThan(0.05f)
    }

    @Test
    fun `energy values stay within zero one`() = runTest {
        val engine = AmbientVisualEngine(backgroundScope, tickMs = 50)
        engine.setPlaying(true)
        advanceTimeBy(60_000); runCurrent()
        val e = engine.state.value.energy
        listOf(e.bass, e.mid, e.treble, e.beat, e.overall).forEach {
            assertThat(it).isAtLeast(0f)
            assertThat(it).isAtMost(1f)
        }
    }

    @Test
    fun `smoothToward attacks faster than it releases`() {
        val start = AmbientEnergy(bass = 0f, mid = 0f, treble = 0f, beat = 0f, overall = 0f)
        val target = AmbientEnergy(bass = 1f, mid = 1f, treble = 1f, beat = 1f, overall = 1f)
        val attacked = start.smoothToward(target)
        val dropped = target.smoothToward(start)
        assertThat(attacked.bass).isGreaterThan(1f - dropped.bass)
    }
}
