package com.sonara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.SubcomposeAsyncImage
import com.sonara.ambient.generatedAmbientPalette
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Artwork with the four required states. When [model] is null — the case for
 * every bundled demo track until the catalog stage — a deterministic palette
 * gradient stands in, so each track still wears its own color identity.
 * Coil handles loading/error internally; both degrade to the same placeholder.
 */
@Composable
fun SonaraArtwork(
    mediaId: String?,
    modifier: Modifier = Modifier,
    model: Any? = null,
    shape: Shape,
    contentDescriptionText: String? = null,
) {
    val placeholderColors = remember(mediaId) { artworkGradient(mediaId) }
    val modifierWithSemantics = if (contentDescriptionText != null) {
        modifier.semantics { contentDescription = contentDescriptionText }
    } else {
        modifier
    }

    if (model == null) {
        Box(
            modifier = modifierWithSemantics
                .clip(shape)
                .background(Brush.linearGradient(placeholderColors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(SonaraSpacing.xl),
                tint = LocalSonaraColors.current.textMuted,
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescriptionText,
            contentScale = ContentScale.Crop,
            modifier = modifierWithSemantics.clip(shape),
            loading = { ArtworkFallback(placeholderColors) },
            error = { ArtworkFallback(placeholderColors) },
        )
    }
}

@Composable
private fun ArtworkFallback(colors: List<Color>) {
    Box(
        modifier = Modifier.background(Brush.linearGradient(colors)),
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

/** Two-stop gradient derived from the track's ambient palette. */
fun artworkGradient(mediaId: String?): List<Color> {
    if (mediaId == null) {
        return listOf(Color(0xFF262733), Color(0xFF131319))
    }
    val palette = generatedAmbientPalette(mediaId)
    return listOf(
        palette.highlight.copy(alpha = 1f),
        palette.base,
    )
}
