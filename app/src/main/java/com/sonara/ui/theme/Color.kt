package com.sonara.ui.theme

import androidx.compose.ui.graphics.Color

// Core ambient palette. The reactive background overrides most on-screen color at
// runtime; these are the stable fallbacks used by Material components and text.
val SonaraBackground = Color(0xFF0B0B0F)
val SonaraSurface = Color(0xFF15151C)
val SonaraSurfaceVariant = Color(0xFF20212B)
val SonaraOnBackground = Color(0xFFF2F2F7)
val SonaraOnSurfaceMuted = Color(0xFFA8A8B3)
val SonaraPrimary = Color(0xFF8E7BFF)
val SonaraOnPrimary = Color(0xFF0B0B0F)
val SonaraAccent = Color(0xFF4FD8D2)
val SonaraError = Color(0xFFFF6B6B)

// Extended roles for layered ambient surfaces. Kept neutral and translucent so
// artwork-derived colors (later milestones) can glow through them.
val SonaraElevatedSurface = Color(0xFF1C1D28)
val SonaraPlayerSurface = Color(0xFF101118)
val SonaraGlassSurface = Color(0x14FFFFFF)
val SonaraGlassBorder = Color(0x24FFFFFF)

// Subtle gradients used by placeholder artwork and hero surfaces.
val SonaraArtworkGradientTop = Color(0xFF262733)
val SonaraArtworkGradientBottom = Color(0xFF131319)
