package com.sonara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors
import com.sonara.ui.theme.SonaraArtworkGradientBottom
import com.sonara.ui.theme.SonaraArtworkGradientTop

/**
 * Placeholder for album artwork. Stands in until the catalog stage supplies
 * real images; keeps a calm gradient + note glyph so layouts read as "music".
 */
@Composable
fun ArtworkPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = SonaraShapes.medium,
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(SonaraArtworkGradientTop, SonaraArtworkGradientBottom),
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(SonaraSpacing.xl),
            tint = LocalSonaraColors.current.textMuted,
        )
    }
}
