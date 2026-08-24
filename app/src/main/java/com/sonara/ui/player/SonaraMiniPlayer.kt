package com.sonara.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.playback.NowPlayingState
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SonaraLiquidGlass
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Compact now-playing bar docked above the navigation bar. Hidden entirely
 * while nothing is loaded; a hairline progress line tracks playback. Tapping
 * anywhere expands into Now Playing; play/pause acts immediately without
 * navigation.
 */
@Composable
fun SonaraMiniPlayer(
    state: NowPlayingState,
    engine: AmbientVisualEngine,
    onTogglePlayPause: () -> Unit,
    onSeekToNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !state.isEmpty && state.title.isNotBlank(),
        enter = fadeIn(tween(SonaraMotion.Normal)),
        exit = fadeOut(tween(SonaraMotion.Normal)),
        modifier = modifier,
    ) {
        val colors = LocalSonaraColors.current
        SonaraLiquidGlass(
            engine = engine,
            shape = SonaraShapes.card,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand,
                )
                .semantics { contentDescription = "Now playing: ${state.title} by ${state.artist}" },
        ) {
            Column {
            Row(
                modifier = Modifier
                    .height(SonaraSpacing.huge)
                    .padding(horizontal = SonaraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
            ) {
                SonaraArtwork(
                    mediaId = state.mediaId,
                    shape = SonaraShapes.small,
                    modifier = Modifier
                        .size(SonaraSpacing.xxl)
                        .padding(SonaraSpacing.xxs),
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
                        text = state.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PlayPauseIcon(
                    isPlaying = state.isPlaying,
                    onToggle = onTogglePlayPause,
                    tint = colors.textPrimary,
                )
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(SonaraSpacing.xl)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSeekToNext,
                        ),
                )
            }

            // Hairline progress line along the bottom edge of the bar.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SonaraSpacing.xxs),
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
    }
}

@Composable
internal fun PlayPauseIcon(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    tint: androidx.compose.ui.graphics.Color,
    size: androidx.compose.ui.unit.Dp = SonaraSpacing.xl,
) {
    Icon(
        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        contentDescription = if (isPlaying) "Pause" else "Play",
        tint = tint,
        modifier = Modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
    )
}
