package com.sonara.playback

/**
 * Pure mapping from MediaController snapshots to [NowPlayingState].
 *
 * The critical rule: a loaded MediaItem must survive transient callbacks.
 * Media3 can briefly report a null currentMediaItem while the timeline is
 * being reorganized (set/seek/remove, track transitions) and reports
 * isPlaying=false while buffering. Neither is "no track". The mini-player
 * may only disappear when the queue genuinely has no items.
 */
object PlaybackStateMapper {

    fun map(
        current: NowPlayingState,
        mediaId: String?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        mediaItemCount: Int,
    ): NowPlayingState {
        val track = mediaId?.let(DemoCatalog::trackById)
        return when {
            track == null && mediaItemCount == 0 ->
                // Genuinely nothing loaded — the only state that hides the player.
                NowPlayingState.Empty.copy(isConnected = current.isConnected)

            track == null ->
                // Transient null mid-transition: retain the last known track.
                current.copy(isPlaying = isPlaying, positionMs = positionMs.coerceAtLeast(0L))

            else ->
                current.copy(
                    isConnected = true,
                    mediaId = track.id,
                    title = track.title,
                    artist = track.artist,
                    isPlaying = isPlaying,
                    positionMs = positionMs.coerceAtLeast(0L),
                    durationMs = durationMs.takeIf { it > 0 } ?: current.durationMs,
                )
        }
    }
}
