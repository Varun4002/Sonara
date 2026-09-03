package com.sonara.ui.material

import androidx.compose.runtime.staticCompositionLocalOf
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ui.theme.GlassTint
import com.sonara.ui.theme.SonaraGlassBorder
import com.sonara.ui.theme.SonaraGlassSurface

/**
 * Glass context provided by the shell so any surface can render the material
 * without threading the engine through every screen. Neutral defaults keep
 * previews and non-shell usage graceful.
 */
val LocalAmbientEngine = staticCompositionLocalOf<AmbientVisualEngine?> { null }

val LocalGlassTint = staticCompositionLocalOf {
    GlassTint(surface = SonaraGlassSurface, border = SonaraGlassBorder)
}
