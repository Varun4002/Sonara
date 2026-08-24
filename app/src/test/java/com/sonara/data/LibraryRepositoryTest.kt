package com.sonara.data

import com.google.common.truth.Truth.assertThat
import com.sonara.playback.NowPlayingState
import org.junit.Test

class LibraryRepositoryTest {

    private fun playing(mediaId: String) = NowPlayingState.Empty.copy(
        mediaId = mediaId,
        title = mediaId,
        artist = "Sonara Sessions",
    )

    @Test
    fun `toggle like flips state and reports it`() {
        val repo = LibraryRepository()
        assertThat(repo.toggleLike("aurora")).isTrue()
        assertThat(repo.isLiked("aurora")).isTrue()
        assertThat(repo.toggleLike("aurora")).isFalse()
        assertThat(repo.isLiked("aurora")).isFalse()
    }

    @Test
    fun `null media id is never liked`() {
        val repo = LibraryRepository()
        assertThat(repo.isLiked(null)).isFalse()
    }

    @Test
    fun `history records plays newest first`() {
        val repo = LibraryRepository()
        repo.recordPlay(playing("aurora"), nowMs = 1_000)
        repo.recordPlay(playing("drift"), nowMs = 2_000)
        repo.recordPlay(playing("halo"), nowMs = 3_000)
        assertThat(repo.history.value.map { it.mediaId })
            .containsExactly("halo", "drift", "aurora")
            .inOrder()
    }

    @Test
    fun `consecutive replays of the same track collapse`() {
        val repo = LibraryRepository()
        repo.recordPlay(playing("aurora"), nowMs = 1_000)
        repo.recordPlay(playing("aurora"), nowMs = 2_000)
        assertThat(repo.history.value).hasSize(1)
    }

    @Test
    fun `replay after another track moves it back to front`() {
        val repo = LibraryRepository()
        repo.recordPlay(playing("aurora"), nowMs = 1_000)
        repo.recordPlay(playing("drift"), nowMs = 2_000)
        repo.recordPlay(playing("aurora"), nowMs = 3_000)
        assertThat(repo.history.value.map { it.mediaId })
            .containsExactly("aurora", "drift")
            .inOrder()
    }

    @Test
    fun `null playback is ignored`() {
        val repo = LibraryRepository()
        repo.recordPlay(NowPlayingState.Empty, nowMs = 1_000)
        assertThat(repo.history.value).isEmpty()
    }

    @Test
    fun `clear history empties it`() {
        val repo = LibraryRepository()
        repo.recordPlay(playing("aurora"), nowMs = 1_000)
        repo.clearHistory()
        assertThat(repo.history.value).isEmpty()
    }
}
