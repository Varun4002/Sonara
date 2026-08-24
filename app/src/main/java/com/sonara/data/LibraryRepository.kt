package com.sonara.data

import com.sonara.playback.NowPlayingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One entry of the in-session listening history. */
data class HistoryEntry(
    val mediaId: String,
    val playedAtMs: Long,
)

/**
 * Session-scoped personal state: liked songs and listening history. Backed by
 * memory until the persistence stage; every mutation is real and immediately
 * reflected in Home, Library, Now Playing and the Liked collection.
 */
class LibraryRepository {

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    fun isLiked(mediaId: String?): Boolean =
        mediaId != null && mediaId in _likedIds.value

    /** Returns the new liked state. */
    fun toggleLike(mediaId: String): Boolean {
        val nowLiked = mediaId !in _likedIds.value
        _likedIds.value = if (nowLiked) {
            _likedIds.value + mediaId
        } else {
            _likedIds.value - mediaId
        }
        return nowLiked
    }

    /** Records a playback start; consecutive replays of the same track collapse. */
    fun recordPlay(state: NowPlayingState, nowMs: Long) {
        val mediaId = state.mediaId ?: return
        val current = _history.value
        if (current.firstOrNull()?.mediaId == mediaId) return
        _history.value = listOf(HistoryEntry(mediaId, nowMs)) +
            current.filterNot { it.mediaId == mediaId }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
