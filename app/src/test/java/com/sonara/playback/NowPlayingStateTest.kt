package com.sonara.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NowPlayingStateTest {

    @Test
    fun `empty state has no media and hides progress`() {
        val empty = NowPlayingState.Empty
        assertThat(empty.isEmpty).isTrue()
        assertThat(empty.progressFraction).isEqualTo(0f)
        assertThat(empty.positionLabel()).isEqualTo("0:00")
    }

    @Test
    fun `duration formats minutes and seconds`() {
        assertThat(NowPlayingState.formatDuration(0L)).isEqualTo("0:00")
        assertThat(NowPlayingState.formatDuration(5_000L)).isEqualTo("0:05")
        assertThat(NowPlayingState.formatDuration(65_000L)).isEqualTo("1:05")
        assertThat(NowPlayingState.formatDuration(3_601_000L)).isEqualTo("60:01")
        assertThat(NowPlayingState.formatDuration(-1L)).isEqualTo("0:00")
    }

    @Test
    fun `progress fraction clamps to zero one`() {
        val state = NowPlayingState(
            isConnected = true,
            mediaId = "aurora",
            title = "Aurora",
            artist = "Sonara Sessions",
            isPlaying = false,
            positionMs = 7_000L,
            durationMs = 14_000L,
        )
        assertThat(state.progressFraction).isWithin(1e-6f).of(0.5f)
        assertThat(state.copy(positionMs = -100L).progressFraction).isEqualTo(0f)
        assertThat(state.copy(positionMs = 999_999L).progressFraction).isEqualTo(1f)
    }

    @Test
    fun `unknown duration yields zero progress instead of NaN`() {
        val state = NowPlayingState.Empty.copy(mediaId = "x", positionMs = 4_000L, durationMs = 0L)
        assertThat(state.progressFraction).isEqualTo(0f)
    }
}
