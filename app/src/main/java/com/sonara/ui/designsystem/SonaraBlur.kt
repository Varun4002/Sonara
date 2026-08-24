package com.sonara.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Blur-radius tokens for frosted surfaces. Rendering constants, not spacing —
 * kept here so no ad-hoc dp values appear in composables.
 */
object SonaraBlur {
    /** Backdrop frost for liquid-glass panels. */
    val glass: Dp = 18.dp

    /** Softer frost for large sheets. */
    val sheet: Dp = 28.dp
}
