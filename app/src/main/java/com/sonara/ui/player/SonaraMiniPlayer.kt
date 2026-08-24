package com.sonara.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.playback.NowPlayingState
import com.sonara.ui.components.ArtworkPlaceholder
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Compact now-playing bar docked above the navigation bar. Hidden entirely
 * while nothing is loaded; a thin progress line tracks playback.
 */
@Composable
fun SonaraMiniPlayer(
    state: NowPlayingState,
    onTogglePlayPause: () -> Unit,
    onSeekToNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isEmpty || state.title.isBlank()) return

    val colors = LocalSonaraColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .sonaraGlass(shape = SonaraShapes.card),
    ) {
        Row(
            modifier = Modifier
                .height(SonaraSpacing.huge)
                .padding(horizontal = SonaraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        ) {
            ArtworkPlaceholder(
                modifier = Modifier
                    .size(SonaraSpacing.xxl)
                    .padding(2.dp),
                shape = SonaraShapes.small,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${state.positionLabel()} / ${state.durationLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = colors.textPrimary,
                modifier = Modifier
                    .clickable(onClick = onTogglePlayPause)
                    .size(SonaraSpacing.xl),
            )
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                tint = colors.textSecondary,
                modifier = Modifier
                    .clickable(onClick = onSeekToNext)
                    .size(SonaraSpacing.xl),
            )
        }

        // Hairline progress line along the bottom edge of the bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(state.progressFraction)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
