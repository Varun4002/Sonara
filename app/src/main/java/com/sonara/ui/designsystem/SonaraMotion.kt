package com.sonara.ui.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Motion tokens. Durations are in milliseconds; call sites pass them to
 * [androidx.compose.animation.core tween] together with an easing from here.
 * No arbitrary durations or easings in composables.
 */
object SonaraMotion {
    /** Press/ripple-scale feedback. */
    const val Fast: Int = 120

    /** Standard in-app transitions (fades, color states). */
    const val Normal: Int = 240

    /** Cross-fade between top-level tabs. */
    const val PageTransition: Int = 280

    /** Mini-player → full player expansion. Reserved for the playback stage. */
    const val PlayerExpand: Int = 380

    /** Slow, deliberate reveals (hero surfaces). */
    const val Slow: Int = 450

    /** Long ambient drifts for the future reactive background. */
    const val Ambient: Int = 800

    /** Confident deceleration — default for entrances and expansions. */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Standard material curve for state changes. */
    val StandardEasing: Easing = FastOutSlowInEasing

    /** Gentle exit curve — faster out than in. */
    val ExitEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
}
