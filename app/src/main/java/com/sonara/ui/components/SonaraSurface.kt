package com.sonara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Restrained glass treatment: translucent fill plus a hairline border. Tint
 * roles come from [LocalSonaraColors], so the ambient engine can shift glass
 * subtly with the current song at the single injection point. No real blur —
 * the fill keeps surfaces legible and cheap everywhere below API 31.
 */
fun Modifier.sonaraGlass(shape: Shape = SonaraShapes.card): Modifier = composed {
    val colors = LocalSonaraColors.current
    this
        .clip(shape)
        .background(colors.glassSurface, shape)
        .border(1.dp, colors.glassBorder, shape)
}
