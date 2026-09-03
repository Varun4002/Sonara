package com.sonara.ambient

import com.sonara.playback.NowPlayingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Bridges playback state to the visual engine: track change → palette
 * transition (interpolated, never an instant swap), play/pause → energy
 * ramp, settings → visual mode. Playback remains owned by the playback
 * layer; this class only observes.
 */
class AmbientEngine(
    val engine: AmbientVisualEngine,
    private val dynamicColorsEnabled: () -> Boolean = { true },
) {
    val state: StateFlow<AmbientVisualState> get() = engine.state

    private var lastMediaId: String? = null

    /** Feed each new playback snapshot (e.g. collected from PlayerConnection). */
    fun onPlaybackState(state: NowPlayingState) {
        val mediaId = state.mediaId
        if (mediaId != lastMediaId) {
            lastMediaId = mediaId
            if (mediaId != null) {
                val target = if (dynamicColorsEnabled()) {
                    generatedAmbientPalette(mediaId)
                } else {
                    AmbientPalette.Neutral
                }
                engine.transitionTo(target)
            } else {
                engine.transitionTo(AmbientPalette.Neutral)
            }
        }
        if (engine.state.value.isPlaying != state.isPlaying) {
            engine.setPlaying(state.isPlaying)
        }
    }

    fun setMode(mode: AmbientVisualMode) = engine.setMode(mode)
}
