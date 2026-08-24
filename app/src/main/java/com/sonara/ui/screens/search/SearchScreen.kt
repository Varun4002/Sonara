package com.sonara.ui.screens.search

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
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.sonara.playback.DemoCatalog
import com.sonara.playback.DemoTrack
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SonaraEmptyState
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors
import kotlinx.coroutines.delay

/**
 * Search over the bundled demo catalog. Debounced local matching with the
 * required states: idle (browse), typing, results, no results. The catalog
 * stage will swap the data source without changing this composition.
 */
@Composable
fun SearchScreen(
    onPlayTrack: (Int) -> Unit,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DemoTrack>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    // Debounce: no matching on every keystroke.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(SonaraMotion.Normal.toLong() * 2)
        results = DemoCatalog.search(query)
        searching = false
    }

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
            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = SonaraSpacing.lg),
            )
        }

        item {
            SearchField(
                query = query,
                onQueryChanged = { query = it },
            )
        }

        if (query.isBlank()) {
            item {
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = SonaraSpacing.sectionGap, bottom = SonaraSpacing.sm),
                )
            }
            item {
                BrowseRow(
                    icon = Icons.Outlined.MusicNote,
                    label = "Songs",
                    detail = "${DemoCatalog.tracks.size} ambient tracks",
                ) { query = "Sonara" }
            }
            item {
                BrowseRow(
                    icon = Icons.Outlined.Album,
                    label = "Albums",
                    detail = DemoCatalog.albums().joinToString(),
                ) { onOpenAlbum(DemoCatalog.albums().first()) }
            }
            item {
                BrowseRow(
                    icon = Icons.Outlined.Person,
                    label = "Artists",
                    detail = DemoCatalog.artists().joinToString(),
                ) { query = DemoCatalog.artists().first() }
            }
        } else if (searching) {
            item {
                Text(
                    text = "Searching…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = SonaraSpacing.lg),
                )
            }
        } else if (results.isEmpty()) {
            item {
                SonaraEmptyState(
                    title = "No matches",
                    body = "Try a different song, artist, or album.",
                    modifier = Modifier.padding(top = SonaraSpacing.xxxl),
                )
            }
        } else {
            item {
                Text(
                    text = "Top result",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = SonaraSpacing.lg, bottom = SonaraSpacing.xxs),
                )
            }
            item {
                val top = results.first()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == top.id }) }
                        .padding(vertical = SonaraSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SonaraArtwork(
                        mediaId = top.id,
                        shape = SonaraShapes.medium,
                        modifier = Modifier.size(SonaraSpacing.massive),
                    )
                    Column {
                        Text(
                            text = top.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Song · ${top.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            if (results.size > 1) {
                item {
                    Text(
                        text = "Songs",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = SonaraSpacing.lg, bottom = SonaraSpacing.xxs),
                    )
                }
                items(results.size - 1) { index ->
                    val track = results[index + 1]
                    SonaraTrackRow(
                        mediaId = track.id,
                        title = track.title,
                        subtitle = track.artist,
                        onClick = { onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == track.id }) },
                    )
                }
            }
        }
    }
}

/** Rounded tonal search field; focus deepens the surface, no neon outline. */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val colors = LocalSonaraColors.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        textStyle = TextStyle(
            color = colors.textPrimary,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sonaraGlass(shape = SonaraShapes.medium)
                    .padding(horizontal = SonaraSpacing.lg, vertical = SonaraSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(SonaraSpacing.xl),
                )
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search songs, artists, albums",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textMuted,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BrowseRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit,
) {
    val colors = LocalSonaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(SonaraSpacing.xl),
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
