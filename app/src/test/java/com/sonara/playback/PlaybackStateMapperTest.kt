package com.sonara.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression tests for the mini-player visibility contract: a loaded track
 * must survive loading, buffering, playing, paused, and end-of-queue states.
 * The player may only become empty when the queue genuinely has no items.
 */
class PlaybackStateMapperTest {

    private val empty = NowPlayingState.Empty

    @Test
    fun `no items means genuinely empty`() {
        val mapped = PlaybackStateMapper.map(empty, mediaId = null, isPlaying = false, positionMs = 0, durationMs = 0, mediaItemCount = 0)
        assertThat(mapped.isEmpty).isTrue()
    }

    @Test
    fun `loading keeps the player visible with track metadata`() {
        val mapped = PlaybackStateMapper.map(empty, mediaId = "aurora", isPlaying = false, positionMs = 0, durationMs = 0, mediaItemCount = 5)
        assertThat(mapped.isEmpty).isFalse()
        assertThat(mapped.mediaId).isEqualTo("aurora")
        assertThat(mapped.title).isEqualTo("Aurora")
    }

    @Test
    fun `full lifecycle never drops the track`() {
        var state = empty

        // LOADING: item set, not yet playing, duration unknown.
        state = PlaybackStateMapper.map(state, "aurora", isPlaying = false, positionMs = 0, durationMs = 0, mediaItemCount = 5)
        assertThat(state.isEmpty).isFalse()

        // PLAYING.
        state = PlaybackStateMapper.map(state, "aurora", isPlaying = true, positionMs = 500, durationMs = 14_000, mediaItemCount = 5)
        assertThat(state.isPlaying).isTrue()
        assertThat(state.mediaId).isEqualTo("aurora")

        // PAUSED.
        state = PlaybackStateMapper.map(state, "aurora", isPlaying = false, positionMs = 6_000, durationMs = 14_000, mediaItemCount = 5)
        assertThat(state.mediaId).isEqualTo("aurora")
        assertThat(state.isEmpty).isFalse()

        // BUFFERING: isPlaying false, position callback fires, item still loaded.
        state = PlaybackStateMapper.map(state, "aurora", isPlaying = false, positionMs = 6_500, durationMs = 14_000, mediaItemCount = 5)
        assertThat(state.mediaId).isEqualTo("aurora")
        assertThat(state.isEmpty).isFalse()

        // PLAYING again.
        state = PlaybackStateMapper.map(state, "aurora", isPlaying = true, positionMs = 7_000, durationMs = 14_000, mediaItemCount = 5)
        assertThat(state.isPlaying).isTrue()

        // ENDED: last track, no next — stays visible, paused at end.
        state = PlaybackStateMapper.map(state, "aurora", isPlaying = false, positionMs = 14_000, durationMs = 14_000, mediaItemCount = 5)
        assertThat(state.isEmpty).isFalse()
        assertThat(state.mediaId).isEqualTo("aurora")
        assertThat(state.isPlaying).isFalse()
    }

    @Test
    fun `transient null media item during timeline change retains the track`() {
        val playing = PlaybackStateMapper.map(empty, "drift", isPlaying = true, positionMs = 4_000, durationMs = 14_000, mediaItemCount = 5)
        val mapped = PlaybackStateMapper.map(playing, mediaId = null, isPlaying = true, positionMs = 4_200, durationMs = 14_000, mediaItemCount = 5)
        assertThat(mapped.isEmpty).isFalse()
        assertThat(mapped.mediaId).isEqualTo("drift")
        assertThat(mapped.title).isEqualTo("Drift")
        assertThat(mapped.positionMs).isEqualTo(4_200)
    }

    @Test
    fun `unknown media id with items still retains last track`() {
        val playing = PlaybackStateMapper.map(empty, "drift", isPlaying = false, positionMs = 1_000, durationMs = 14_000, mediaItemCount = 5)
        val mapped = PlaybackStateMapper.map(playing, mediaId = "not-in-catalog", isPlaying = false, positionMs = 1_100, durationMs = 14_000, mediaItemCount = 5)
        assertThat(mapped.isEmpty).isFalse()
        assertThat(mapped.mediaId).isEqualTo("drift")
    }

    @Test
    fun `duration is retained when the player reports unknown`() {
        val playing = PlaybackStateMapper.map(empty, "aurora", isPlaying = true, positionMs = 500, durationMs = 14_000, mediaItemCount = 5)
        val buffering = PlaybackStateMapper.map(playing, "aurora", isPlaying = false, positionMs = 600, durationMs = 0, mediaItemCount = 5)
        assertThat(buffering.durationMs).isEqualTo(14_000)
    }

    @Test
    fun `queue cleared while playing ends in empty state`() {
        val playing = PlaybackStateMapper.map(empty, "aurora", isPlaying = true, positionMs = 3_000, durationMs = 14_000, mediaItemCount = 5)
        val cleared = PlaybackStateMapper.map(playing, mediaId = null, isPlaying = false, positionMs = 0, durationMs = 0, mediaItemCount = 0)
        assertThat(cleared.isEmpty).isTrue()
    }

    @Test
    fun `negative positions are clamped`() {
        val mapped = PlaybackStateMapper.map(empty, "aurora", isPlaying = false, positionMs = -5, durationMs = 14_000, mediaItemCount = 1)
        assertThat(mapped.positionMs).isEqualTo(0)
    }
}
