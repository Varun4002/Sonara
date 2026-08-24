package com.sonara.playback

import kotlin.math.floor

/**
 * Immutable playback state consumed by the UI. [Empty] until a controller is
 * connected and a media item is loaded.
 */
data class NowPlayingState(
    val isConnected: Boolean,
    val mediaId: String?,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
) {
    val isEmpty: Boolean get() = mediaId == null

    /** 0f..1f, guarded against zero/unknown duration. */
    val progressFraction: Float
        get() = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else 0f

    fun positionLabel(): String = formatDuration(positionMs)

    fun durationLabel(): String = formatDuration(durationMs)

    companion object {
        val Empty = NowPlayingState(
            isConnected = false,
            mediaId = null,
            title = "",
            artist = "",
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
        )

        /** mm:ss — hours are not expected for ambient tracks but stay correct. */
        fun formatDuration(ms: Long): String {
            val totalSeconds = floor(ms / 1000.0).toInt().coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
