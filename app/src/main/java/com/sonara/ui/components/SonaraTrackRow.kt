package com.sonara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * The single track-row pattern used everywhere (recently played, results,
 * album and playlist tracklists, queue, Flow up next). Rows sit directly on
 * the ambient background — never carded. [isPlaying] marks the current track
 * with an accent note glyph, not color alone.
 */
@Composable
fun SonaraTrackRow(
    mediaId: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkShape: Shape = SonaraShapes.small,
    isPlaying: Boolean = false,
    showMore: Boolean = false,
    onMore: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalSonaraColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = SonaraSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SonaraArtwork(
            mediaId = mediaId,
            model = null,
            shape = artworkShape,
            modifier = Modifier.size(SonaraSpacing.huge),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isPlaying) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Now playing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(SonaraSpacing.lg)
                    .semantics { contentDescription = "Currently playing" },
            )
        }
        trailing?.invoke()
        if (showMore && onMore != null) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More options for $title",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(SonaraSpacing.xl)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMore,
                    ),
            )
        }
    }
}

/** Small circular playing indicator used where the note glyph is too loud. */
@Composable
fun PlayingDot(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(SonaraSpacing.sm)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}
