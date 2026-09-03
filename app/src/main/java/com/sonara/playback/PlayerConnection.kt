package com.sonara.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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

/** One entry of the playback queue as exposed to the UI. */
data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val isCurrent: Boolean,
)

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

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var controller: MediaController? = null
    private var positionTicker: Job? = null

    /** Metadata for provider-backed tracks not present in DemoCatalog. */
    private val remoteMeta = mutableMapOf<String, Pair<String, String>>()

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

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    refreshQueue()
                }

                override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                    _shuffleEnabled.value = enabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }
            },
        )
        refreshFromPlayer()
        refreshQueue()
        _shuffleEnabled.value = connected.shuffleModeEnabled
        _repeatMode.value = connected.repeatMode
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

    /**
     * Plays provider-backed tracks. Stream URIs are resolved via [resolver]
     * off the main thread before the queue is set; tracks whose source can't
     * be resolved are skipped. Falls back cleanly — the demo path is untouched.
     */
    fun playTracks(
        tracks: List<com.sonara.music.Track>,
        startIndex: Int,
        resolver: suspend (com.sonara.music.Track) -> String?,
    ) {
        scope.launch {
            val items = tracks.mapNotNull { track ->
                val raw = resolver(track) ?: return@mapNotNull null
                val uri = if (raw.startsWith("demo:")) {
                    "android.resource://${appContext.packageName}/raw/${raw.removePrefix("demo:")}"
                } else raw
                remoteMeta[track.id] = track.title to track.artist
                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(uri)
                    .build()
            }
            if (items.isEmpty()) return@launch
            val index = startIndex.coerceIn(0, items.lastIndex)
            controller?.run {
                setMediaItems(items, index, 0L)
                prepare()
                play()
            }
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

    /** Removes every upcoming item; the current track keeps playing. */
    fun clearUpNext() {
        controller?.run {
            val current = currentMediaItemIndex
            removeMediaItems((current + 1).coerceAtMost(mediaItemCount), mediaItemCount)
        }
        refreshQueue()
    }

    fun playQueueIndex(index: Int) {
        controller?.run {
            seekTo(index, 0L)
            play()
        }
    }

    /** Toggles Media3 shuffle; the session is the source of truth. */
    fun toggleShuffle() {
        controller?.run { shuffleModeEnabled = !shuffleModeEnabled }
    }

    /** Cycles OFF → ONE → ALL → OFF. */
    fun cycleRepeatMode() {
        controller?.run {
            repeatMode = when (repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
        }
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

    private fun refreshQueue() {
        val player = controller ?: return
        val timeline = player.currentTimeline
        if (timeline.isEmpty) {
            _queue.value = emptyList()
            return
        }
        val current = player.currentMediaItemIndex
        _queue.value = buildList {
            for (i in 0 until timeline.windowCount) {
                val mediaId = timeline.getWindow(i, Timeline.Window()).mediaItem.mediaId
                    ?: continue
                val demo = DemoCatalog.trackById(mediaId)
                val remote = remoteMeta[mediaId]
                if (demo == null && remote == null) continue
                add(
                    QueueEntry(
                        mediaId = mediaId,
                        title = demo?.title ?: remote!!.first,
                        artist = demo?.artist ?: remote!!.second,
                        isCurrent = i == current,
                    ),
                )
            }
        }
    }

    private fun refreshFromPlayer() {
        val player = controller ?: return
        val mediaId = player.currentMediaItem?.mediaId
        val remote = mediaId?.let { remoteMeta[it] }
        _state.update {
            PlaybackStateMapper.map(
                current = it,
                mediaId = mediaId,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition,
                durationMs = player.duration,
                mediaItemCount = player.mediaItemCount,
                remoteTitle = remote?.first,
                remoteArtist = remote?.second,
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
