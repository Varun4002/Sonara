package com.sonara.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.data.LibraryRepository
import com.sonara.music.MusicRepository
import com.sonara.playback.DemoCatalog
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SectionHeader
import com.sonara.ui.designsystem.LocalBottomChromeHeight
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors
import com.sonara.ui.theme.SonaraPrimary

/**
 * Library — the personal space. Liked songs get a distinctive treatment;
 * downloads, albums and artists stay informational and calm. History lives
 * in its own grouped section.
 */
@Composable
fun LibraryScreen(
    library: LibraryRepository,
    onPlayTrack: (Int) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSettings: () -> Unit,
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val liked by library.likedIds.collectAsState()
    val playlists by musicRepo.playlists.collectAsState()
    val albums = remember { DemoCatalog.albums() }

    LaunchedEffect(musicRepo) { musicRepo.refreshPlaylists() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = SonaraSpacing.screenPadding,
            top = SonaraSpacing.xxl,
            end = SonaraSpacing.screenPadding,
            bottom = LocalBottomChromeHeight.current,
        ),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(SonaraSpacing.xl)
                        .clickable(onClick = onOpenSettings),
                )
            }
        }

        item {
            // Liked songs — visually distinct from ordinary rows.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenLiked)
                    .padding(vertical = SonaraSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(SonaraSpacing.huge)
                        .clip(SonaraShapes.medium)
                        .background(
                            Brush.linearGradient(
                                listOf(SonaraPrimary.copy(alpha = 0.7f), Color(0xFF3A2B66)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(SonaraSpacing.xl),
                    )
                }
                Column {
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${liked.size} ${if (liked.size == 1) "song" else "songs"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        item {
            InfoRow(
                icon = Icons.Outlined.Download,
                title = "Downloads",
                detail = "0 songs · offline playback arrives with the catalog stage",
            )
        }

        if (playlists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Playlists",
                    modifier = Modifier.padding(top = SonaraSpacing.lg),
                )
            }
            items(playlists.size) { index ->
                val playlist = playlists[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SonaraSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SonaraArtwork(
                        mediaId = playlist.id,
                        model = playlist.artworkUrl,
                        shape = SonaraShapes.small,
                        modifier = Modifier.size(SonaraSpacing.huge),
                    )
                    Column {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${playlist.trackCount} ${if (playlist.trackCount == 1) "song" else "songs"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Albums",
                modifier = Modifier.padding(top = SonaraSpacing.lg),
            )
        }
        items(albums.size) { index ->
            val album = albums[index]
            val tracks = DemoCatalog.tracksForAlbum(album)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlbum(album) }
                    .padding(vertical = SonaraSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SonaraArtwork(
                    mediaId = tracks.first().id,
                    shape = SonaraShapes.small,
                    modifier = Modifier.size(SonaraSpacing.huge),
                )
                Column {
                    Text(
                        text = album,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${tracks.first().year} · ${tracks.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Artists",
                modifier = Modifier.padding(top = SonaraSpacing.lg),
            )
        }
        items(DemoCatalog.artists().size) { index ->
            val artist = DemoCatalog.artists()[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenArtist(artist) }
                    .padding(vertical = SonaraSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(SonaraSpacing.huge)
                        .clip(CircleShape)
                        .background(colors.elevatedSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(SonaraSpacing.xl),
                    )
                }
                Column {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${DemoCatalog.tracksForArtist(artist).size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, detail: String) {
    val colors = LocalSonaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonaraSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SonaraSpacing.huge)
                .clip(SonaraShapes.medium)
                .background(colors.elevatedSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(SonaraSpacing.xl),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
    }
}
