package com.sonara.ui.screens.home

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
import com.sonara.data.LibraryRepository
import com.sonara.playback.DemoCatalog
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SectionHeader
import com.sonara.ui.components.SonaraEmptyState
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.components.sonaraPressScale
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Home — the primary discovery surface. A calm, editorial top region with
 * generous breathing room, large artwork-led featured cards, and a compact
 * session history. The ambient background stays behind; nothing scrolls it.
 */
@Composable
fun HomeScreen(
    onPlayTrack: (Int) -> Unit,
    library: LibraryRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val history by library.history.collectAsState()
    val featured = DemoCatalog.tracks.take(2)
    val recent = history.mapNotNull { entry -> DemoCatalog.trackById(entry.mediaId) }

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
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = SonaraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xl),
            ) {
                // Brand label, deliberately quiet; the hero carries the screen.
                Text(
                    text = "Sonara",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Text(
                    text = "Listen ambiently",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                )
            }
        }

        item {
            SectionHeader(
                title = "Made for tonight",
                action = "See all",
                modifier = Modifier.padding(top = SonaraSpacing.lg),
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md)) {
                featured.forEachIndexed { index, track ->
                    FeaturedCard(
                        mediaId = track.id,
                        title = track.title,
                        onClick = { onPlayTrack(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Recently played",
                action = if (recent.isNotEmpty()) "History" else null,
                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
            )
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    text = "Your listening history will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = SonaraSpacing.sm),
                )
            }
        } else {
            items(recent.size) { index ->
                val track = recent[index]
                SonaraTrackRow(
                    mediaId = track.id,
                    title = track.title,
                    subtitle = track.artist,
                    isPlaying = false,
                    onClick = {
                        onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == track.id })
                    },
                )
            }
        }
    }
}

/** Large artwork-led card: artwork, title, nothing else. Press scales subtly. */
@Composable
private fun FeaturedCard(
    mediaId: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .sonaraPressScale(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
    ) {
        SonaraArtwork(
            mediaId = mediaId,
            shape = SonaraShapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentDescriptionText = "Play $title",
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
