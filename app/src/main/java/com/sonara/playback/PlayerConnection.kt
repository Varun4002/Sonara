package com.sonara.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-facing handle on the playback session. Wraps an async [MediaController]
 * connection and exposes playback as a single [StateFlow] of [NowPlayingState].
 *
 * The controller talks to [PlaybackService], so audio keeps playing while the
 * activity comes and goes.
 */
class PlayerConnection(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(NowPlayingState.Empty)
    val state: StateFlow<NowPlayingState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionTicker: Job? = null

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                val connected = try {
                    future.get()
                } catch (t: Exception) {
                    null // session service unavailable; state stays Empty
                }
                connected?.let { onConnected(it) }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun onConnected(connected: MediaController) {
        controller = connected
        connected.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    refreshFromPlayer()
                    if (isPlaying) startPositionTicker() else stopPositionTicker()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    refreshFromPlayer()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    refreshFromPlayer()
                }
            },
        )
        refreshFromPlayer()
    }

    /** Loads the demo playlist and starts playback at [index]. */
    fun playTrack(index: Int) {
        val items = DemoCatalog.tracks.map { it.toMediaItem(appContext.packageName) }
        controller?.run {
            setMediaItems(items, index, 0L)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        controller?.run { if (isPlaying) pause() else play() }
    }

    fun seekToNext() {
        controller?.seekToNextMediaItem()
    }

    fun seekToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /**
     * Releases this activity-scoped controller only; the service keeps its own
     * player alive so audio continues in the background.
     */
    fun dispose() {
        stopPositionTicker()
        controller?.release()
        controller = null
    }

    private fun refreshFromPlayer() {
        val player = controller ?: return
        val track = player.currentMediaItem?.mediaId?.let(DemoCatalog::trackById)
        _state.update {
            it.copy(
                isConnected = true,
                mediaId = track?.id,
                title = track?.title.orEmpty(),
                artist = track?.artist.orEmpty(),
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs,
            )
        }
    }

    private fun startPositionTicker() {
        if (positionTicker?.isActive == true) return
        positionTicker = scope.launch {
            while (isActive) {
                delay(POSITION_TICK_MS)
                val player = controller ?: break
                _state.update { it.copy(positionMs = player.currentPosition) }
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    private companion object {
        const val POSITION_TICK_MS = 500L
    }
}

/** One-shot mapping from a demo track to a playable MediaItem. */
private fun DemoTrack.toMediaItem(packageName: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri("android.resource://$packageName/raw/$resourceResName")
        .build()
