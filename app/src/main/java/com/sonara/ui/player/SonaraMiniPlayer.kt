package com.sonara.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.playback.NowPlayingState
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.material.LiquidGlassDimensions
import com.sonara.ui.material.LiquidGlassSurface
import com.sonara.ui.material.LiquidGlassTokens
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Compact now-playing bar docked above the navigation glass. Visibility is
 * driven solely by [NowPlayingState.hasCurrentTrack] — paused, buffering and
 * ended all keep the material on screen; only an empty queue removes it. The
 * bar emerges from the navigation area and retreats into it; tapping expands
 * into Now Playing while play/pause acts immediately.
 */
@Composable
fun SonaraMiniPlayer(
    state: NowPlayingState,
    onTogglePlayPause: () -> Unit,
    onSeekToNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.hasCurrentTrack,
        enter = slideInVertically(
            animationSpec = tween(SonaraMotion.MiniPlayerEnter, easing = SonaraMotion.EmphasizedEasing),
            initialOffsetY = { it },
        ) + fadeIn(tween(SonaraMotion.Normal)),
        exit = slideOutVertically(
            animationSpec = tween(SonaraMotion.MiniPlayerExit, easing = SonaraMotion.ExitEasing),
            targetOffsetY = { it },
        ) + fadeOut(tween(SonaraMotion.Normal)),
        modifier = modifier,
    ) {
        val colors = LocalSonaraColors.current
        LiquidGlassSurface(
            shape = SonaraShapes.glassMiniPlayer,
            cornerRadius = LiquidGlassDimensions.miniPlayerRadius,
            intensity = LiquidGlassTokens.Standard,
            modifier = Modifier
                .fillMaxWidth()
                .height(LiquidGlassDimensions.miniPlayerHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand,
                )
                .semantics {
                    contentDescription = "Now playing: ${state.title} by ${state.artist}"
                },
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .height(LiquidGlassDimensions.miniPlayerHeight)
                        .padding(horizontal = SonaraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                ) {
                    SonaraArtwork(
                        mediaId = state.mediaId,
                        shape = SonaraShapes.medium,
                        modifier = Modifier
                            .size(LiquidGlassDimensions.miniPlayerArtwork)
                            .clip(SonaraShapes.medium),
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
                    // Primary control: a soft circular glass button, not a
                    // filled Material icon button.
                    Box(
                        modifier = Modifier
                            .size(SonaraSpacing.xxl)
                            .clip(CircleShape)
                            .background(colors.textPrimary.copy(alpha = 0.14f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTogglePlayPause,
                            )
                            .semantics {
                                contentDescription = if (state.isPlaying) "Pause" else "Play"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayPauseGlyph(
                            isPlaying = state.isPlaying,
                            tint = colors.textPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(SonaraSpacing.huge)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSeekToNext,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next track",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(SonaraSpacing.xl),
                        )
                    }
                }

                // Hairline progress along the glass's bottom edge; inherits
                // the ambient accent. Visible while paused too.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(state.progressFraction)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                    )
                }
            }
        }
    }
}

/** Play/pause glyph only — interaction lives on the surrounding touch zone. */
@Composable
fun PlayPauseGlyph(isPlaying: Boolean, tint: Color) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (scaleIn(initialScale = 0.6f, animationSpec = tween(SonaraMotion.Fast)) +
                fadeIn(tween(SonaraMotion.Fast)))
                .togetherWith(scaleOut(targetScale = 0.6f, animationSpec = tween(SonaraMotion.Fast)) +
                    fadeOut(tween(SonaraMotion.Fast)))
        },
        label = "mini-play-pause",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(SonaraSpacing.lg),
        )
    }
}

/**
 * Shared play/pause control used where no larger touch zone wraps the glyph
 * (e.g. Flow's session card): a 48dp tappable box with the morphing glyph.
 */
@Composable
fun PlayPauseIcon(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(SonaraSpacing.huge)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PlayPauseGlyph(isPlaying = isPlaying, tint = tint)
    }
}
