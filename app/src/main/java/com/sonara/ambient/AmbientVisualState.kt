package com.sonara.ambient

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** How much visual life the environment is allowed to have. */
enum class AmbientVisualMode {
    /** Full reactive environment. */
    On,

    /** Same palette, fewer/slower fields. */
    Reduced,

    /** Static neutral atmosphere; no animation loop. */
    Off,
}

/**
 * Smoothed audio-energy signals that modulate the environment. All values are
 * 0f..1f and exponentially smoothed — raw samples never reach Compose.
 *
 * [bass] drives large-scale liquid displacement, [mid] flow deformation,
 * [treble] subtle highlights, [beat] a small environmental pulse and [overall]
 * the global intensity. A real analyser can feed these later via
 * [EnergySource]; today they are derived from playback state with organic
 * drift, which keeps the environment honest (calm when idle, alive when
 * playing) without pretending to read the waveform.
 */
data class AmbientEnergy(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f,
    val beat: Float = 0f,
    val overall: Float = 0f,
) {
    companion object {
        val Resting = AmbientEnergy()
    }
}

/** Pluggable source of raw (unsmoothed) energy targets, 0f..1f. */
interface EnergySource {
    suspend fun sample(isPlaying: Boolean, timeMs: Long): AmbientEnergy
}

/**
 * Playback-state-derived energy with slow organic oscillation. Deliberately
 * low-frequency: the environment breathes rather than jitters.
 */
class PlaybackStateEnergySource : EnergySource {
    override suspend fun sample(isPlaying: Boolean, timeMs: Long): AmbientEnergy {
        if (!isPlaying) return AmbientEnergy.Resting
        val t = timeMs / 1000f
        fun wave(period: Float, phase: Float) =
            ((kotlin.math.sin(t / period * 2f * Math.PI.toFloat() + phase) + 1f) / 2f)
        val bass = 0.45f + 0.30f * wave(11f, 0f)
        val mid = 0.35f + 0.25f * wave(7f, 2.1f)
        val treble = 0.20f + 0.18f * wave(5f, 4.2f)
        val beat = (wave(3.5f, 1.0f) - 0.55f).coerceAtLeast(0f) / 0.45f
        return AmbientEnergy(
            bass = bass,
            mid = mid,
            treble = treble,
            beat = beat,
            overall = (bass + mid) / 2f,
        )
    }
}

/**
 * Single source of truth for the visual engine: current palette, transition
 * progress toward it, and smoothed energy. Owned by [AmbientEngine]; the
 * renderer only reads.
 */
data class AmbientVisualState(
    val palette: AmbientPalette = AmbientPalette.Neutral,
    /** 0f = showing [previousPalette], 1f = fully transitioned to [palette]. */
    val transition: Float = 1f,
    val previousPalette: AmbientPalette = AmbientPalette.Neutral,
    val energy: AmbientEnergy = AmbientEnergy.Resting,
    val isPlaying: Boolean = false,
    val mode: AmbientVisualMode = AmbientVisualMode.On,
) {
    /** Palette lerped at [transition] — what the renderer actually draws. */
    fun resolved(): AmbientPalette {
        if (transition >= 1f) return palette
        val t = transition
        fun lerpC(a: Color, b: Color) = Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = a.alpha + (b.alpha - a.alpha) * t,
        )
        return AmbientPalette(
            base = lerpC(previousPalette.base, palette.base),
            primary = lerpC(previousPalette.primary, palette.primary),
            secondary = lerpC(previousPalette.secondary, palette.secondary),
            highlight = lerpC(previousPalette.highlight, palette.highlight),
            accent = lerpC(previousPalette.accent, palette.accent),
        )
    }
}

/**
 * Advances palette transitions and energy smoothing on fixed ticks, isolated
 * from both playback and UI. UI observes [state]; nothing recomposes per audio
 * sample because updates arrive at [TICK_MS] granularity.
 */
class AmbientVisualEngine(
    private val scope: CoroutineScope,
    private val energySource: EnergySource = PlaybackStateEnergySource(),
    private val transitionMs: Int = TRANSITION_MS,
    private val tickMs: Long = TICK_MS,
) {
    private val _state = MutableStateFlow(AmbientVisualState())
    val state: StateFlow<AmbientVisualState> = _state.asStateFlow()

    private var transitionJob: Job? = null
    private var energyJob: Job? = null

    /** Begin interpolating from the current visual palette to [target]. */
    fun transitionTo(target: AmbientPalette) {
        val current = _state.value
        if (current.palette == target) return
        transitionJob?.cancel()
        transitionJob = scope.launch {
            val previous = current.resolved()
            _state.value = current.copy(
                previousPalette = previous,
                palette = target,
                transition = 0f,
            )
            val steps = (transitionMs / tickMs).coerceAtLeast(1)
            for (step in 1..steps) {
                delay(tickMs)
                _state.value = _state.value.copy(transition = step.toFloat() / steps)
                if (step == steps) break
            }
        }
    }

    /** Starts (or restarts) the energy loop for the given playback state. */
    fun setPlaying(playing: Boolean) {
        _state.value = _state.value.copy(isPlaying = playing)
        energyJob?.cancel()
        energyJob = scope.launch {
            var smoothed = _state.value.energy
            var start = 0L
            while (isActive) {
                delay(tickMs)
                start += tickMs
                val target = energySource.sample(playing, start)
                smoothed = smoothed.smoothToward(target)
                _state.value = _state.value.copy(energy = smoothed)
            }
        }
    }

    fun setMode(mode: AmbientVisualMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    private companion object {
        const val TRANSITION_MS = 1_400
        const val TICK_MS = 50L
    }
}

/** Exponential smoothing toward [target]; decay is slower than attack. */
fun AmbientEnergy.smoothToward(target: AmbientEnergy, attack: Float = 0.25f, release: Float = 0.06f): AmbientEnergy {
    fun next(current: Float, goal: Float): Float {
        val rate = if (goal > current) attack else release
        return current + (goal - current) * rate
    }
    return AmbientEnergy(
        bass = next(bass, target.bass),
        mid = next(mid, target.mid),
        treble = next(treble, target.treble),
        beat = next(beat, target.beat),
        overall = next(overall, target.overall),
    )
}
