package com.sonara.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale. All UI spacing must come from here — no ad-hoc dp values in
 * composables. Scale steps double as named semantic roles at the bottom.
 */
object SonaraSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 40.dp
    val huge: Dp = 48.dp
    val massive: Dp = 64.dp

    /** Horizontal padding of every screen edge. */
    val screenPadding: Dp = xl

    /** Vertical gap between major content sections. */
    val sectionGap: Dp = xxl

    /** Height of the floating bottom navigation bar. */
    val navBarHeight: Dp = 64.dp
}
