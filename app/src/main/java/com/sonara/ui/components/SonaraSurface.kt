package com.sonara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.theme.SonaraGlassBorder
import com.sonara.ui.theme.SonaraGlassSurface

/**
 * Restrained glass treatment: translucent fill plus a hairline border. No real
 * blur — the visual engine (later stage) will drive backdrop effects; this
 * keeps surfaces legible and cheap everywhere below API 31.
 */
fun Modifier.sonaraGlass(shape: Shape = SonaraShapes.card): Modifier = this
    .clip(shape)
    .background(SonaraGlassSurface, shape)
    .border(1.dp, SonaraGlassBorder, shape)
