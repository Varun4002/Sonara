package com.sonara.ui.screens.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
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
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Album — artwork-led header with the album's own palette, Play/Shuffle
 * actions, and a numbered tracklist. Playing a track starts the album at that
 * position within the demo catalog queue.
 */
@Composable
fun AlbumScreen(
    album: String,
    onPlayTrack: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val tracks = remember(album) { DemoCatalog.tracksForAlbum(album) }
    val firstTrack = tracks.firstOrNull()

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
                    text = "Album",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }
        }

        if (firstTrack != null) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SonaraSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
                ) {
                    SonaraArtwork(
                        mediaId = firstTrack.id,
                        shape = SonaraShapes.large,
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .aspectRatio(1f),
                        contentDescriptionText = "$album album artwork",
                    )
                    Text(
                        text = album,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${firstTrack.artist} · ${firstTrack.year} · ${tracks.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xl),
                        modifier = Modifier.padding(top = SonaraSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumAction(
                            icon = Icons.Filled.PlayArrow,
                            label = "Play",
                            onClick = {
                                onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == firstTrack.id })
                            },
                        )
                        AlbumAction(
                            icon = Icons.Outlined.Shuffle,
                            label = "Shuffle",
                            onClick = {
                                // Deterministic demo catalog: shuffle starts mid-album.
                                val start = (tracks.size - 1).coerceAtLeast(0)
                                onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == tracks[start].id })
                            },
                        )
                    }
                }
            }

            items(tracks.size) { index ->
                val track = tracks[index]
                SonaraTrackRow(
                    mediaId = track.id,
                    title = "${track.trackNumber}. ${track.title}",
                    subtitle = formatTrackLength(track.durationMs),
                    onClick = {
                        onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == track.id })
                    },
                )
            }
        }
    }
}

@Composable
private fun AlbumAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalSonaraColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = SonaraSpacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(SonaraSpacing.xl),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
    }
}

private fun formatTrackLength(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
