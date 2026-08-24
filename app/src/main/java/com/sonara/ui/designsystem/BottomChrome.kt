package com.sonara.ui.designsystem

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Height of the shell's bottom chrome (mini-player + navigation + system
 * gesture inset) as measured by SonaraShell. Screens read this for their
 * bottom content padding so content is never hidden underneath the floating
 * system — and the inset follows the chrome automatically when the
 * mini-player appears or disappears.
 */
val LocalBottomChromeHeight = staticCompositionLocalOf { SonaraSpacing.massive }
