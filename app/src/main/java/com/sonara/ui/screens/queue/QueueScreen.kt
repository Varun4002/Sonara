package com.sonara.ui.screens.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
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
import com.sonara.ui.components.SonaraEmptyState
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Queue — a focused utility screen. Current track on top with a clear playing
 * indicator, upcoming below. "Clear" removes upcoming items only; the current
 * track keeps playing.
 */
@Composable
fun QueueScreen(
    player: PlayerConnection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val queue by player.queue.collectAsState()
    val current = queue.firstOrNull { it.isCurrent }
    val upNext = queue.filterNot { it.isCurrent }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = SonaraSpacing.screenPadding,
            top = SonaraSpacing.xl,
            end = SonaraSpacing.screenPadding,
            bottom = SonaraSpacing.massive,
        ),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .size(SonaraSpacing.xl)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack,
                            ),
                    )
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                }
                if (upNext.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable(onClick = player::clearUpNext),
                    )
                }
            }
        }

        if (queue.isEmpty()) {
            item {
                SonaraEmptyState(
                    title = "Nothing queued",
                    body = "Play something from Home or Search and the queue will build here.",
                    modifier = Modifier.padding(top = SonaraSpacing.xxxl),
                )
            }
        } else {
            current?.let { entry ->
                item(key = "current-${entry.mediaId}") {
                    Column {
                        Text(
                            text = "Now playing",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = SonaraSpacing.lg, bottom = SonaraSpacing.xxs),
                        )
                        SonaraTrackRow(
                            mediaId = entry.mediaId,
                            title = entry.title,
                            subtitle = entry.artist,
                            isPlaying = true,
                            onClick = {},
                        )
                    }
                }
            }
            if (upNext.isNotEmpty()) {
                item(key = "upnext-header") {
                    Text(
                        text = "Up next",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = SonaraSpacing.lg, bottom = SonaraSpacing.xxs),
                    )
                }
                items(upNext.size, key = { upNext[it].mediaId }) { index ->
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
