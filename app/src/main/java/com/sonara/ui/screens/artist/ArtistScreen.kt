package com.sonara.ui.screens.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.playback.DemoCatalog
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SectionHeader
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Artist — identity first. A circular monogram header, Play action, popular
 * tracks and the artist's releases. The demo catalog has a single artist;
 * the composition is catalog-ready for the next stage.
 */
@Composable
fun ArtistScreen(
    onPlayTrack: (Int) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val artist = remember { DemoCatalog.artists().first() }
    val tracks = remember(artist) { DemoCatalog.tracksForArtist(artist) }
    val albums = remember(artist) { DemoCatalog.tracksForArtist(artist).map { it.album }.distinct() }

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
                    text = "Artist",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }
        }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SonaraSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(SonaraSpacing.massive * 2)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = artist.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textPrimary,
                    )
                }
                Text(
                    text = artist,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${tracks.size} tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                    modifier = Modifier
                        .clickable { onPlayTrack(0) }
                        .padding(vertical = SonaraSpacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(SonaraSpacing.xl),
                    )
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Popular",
                modifier = Modifier.padding(top = SonaraSpacing.lg),
            )
        }
        items(tracks.size) { index ->
            val track = tracks[index]
            SonaraTrackRow(
                mediaId = track.id,
                title = track.title,
                subtitle = track.album,
                onClick = { onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == track.id }) },
            )
        }

        item {
            SectionHeader(
                title = "Albums",
                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md)) {
                albums.forEach { album ->
                    val albumTracks = DemoCatalog.tracksForAlbum(album)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAlbum(album) },
                        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                    ) {
                        SonaraArtwork(
                            mediaId = albumTracks.first().id,
                            shape = SonaraShapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                        Text(
                            text = album,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
