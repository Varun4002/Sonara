package com.sonara.ui.screens.flow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.playback.PlayerConnection
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SonaraEmptyState
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.player.PlayPauseIcon
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Flow — Sonara's continuous listening session. With a session active the
 * current track leads and Up next follows; without one, a quiet invitation to
 * start. The ambient environment is at its most visible here by design.
 */
@Composable
fun FlowScreen(
    player: PlayerConnection,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val playback by player.state.collectAsState()
    val queue by player.queue.collectAsState()
    val upNext = queue.filterNot { it.isCurrent }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = SonaraSpacing.screenPadding,
            top = SonaraSpacing.xxl,
            end = SonaraSpacing.screenPadding,
            bottom = SonaraSpacing.massive,
        ),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs)) {
                Text(
                    text = "Flow",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Let the atmosphere carry you.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
        }

        if (playback.isEmpty) {
            item {
                SonaraEmptyState(
                    title = "Start a Flow",
                    body = "Sonara will keep the music going, one ambient track into the next.",
                    action = {
                        FlowPrimaryAction(label = "Start Flow") {
                            player.playTrack(0)
                        }
                    },
                    modifier = Modifier.padding(top = SonaraSpacing.xxxl),
                )
            }
        } else {
            item {
                // Current session track, artwork-led.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SonaraSpacing.xl)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenNowPlaying,
                        ),
                    verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
                ) {
                    SonaraArtwork(
                        mediaId = playback.mediaId,
                        shape = SonaraShapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f),
                        contentDescriptionText = "Now playing artwork",
                    )
                    Text(
                        text = playback.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = playback.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.lg),
                        modifier = Modifier.padding(top = SonaraSpacing.sm),
                    ) {
                        Text(
                            text = if (playback.isPlaying) "Playing" else "Paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                        )
                        PlayPauseIcon(
                            isPlaying = playback.isPlaying,
                            onToggle = player::togglePlayPause,
                            tint = colors.textPrimary,
                        )
                    }
                }
            }

            if (upNext.isNotEmpty()) {
                item {
                    Text(
                        text = "Up next",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = SonaraSpacing.sectionGap, bottom = SonaraSpacing.xxs),
                    )
                }
                items(upNext.size) { index ->
                    val entry = upNext[index]
                    SonaraTrackRow(
                        mediaId = entry.mediaId,
                        title = entry.title,
                        subtitle = entry.artist,
                        onClick = {
                            player.playQueueIndex(
                                player.queue.value.indexOfFirst { it.mediaId == entry.mediaId },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowPrimaryAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .sonaraGlass(shape = SonaraShapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = SonaraSpacing.xxl, vertical = SonaraSpacing.md),
    )
}
