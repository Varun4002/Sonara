package com.sonara.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.playback.DemoCatalog
import com.sonara.ui.components.ArtworkPlaceholder
import com.sonara.ui.components.SectionHeader
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Home tab. Backed by the bundled demo catalog until the catalog stage;
 * tapping an item starts playback.
 */
@Composable
fun HomeScreen(
    onPlayTrack: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val featured = DemoCatalog.tracks.take(2)
    val recent = DemoCatalog.tracks.drop(2)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SonaraSpacing.screenPadding,
            top = SonaraSpacing.xxl,
            end = SonaraSpacing.screenPadding,
            bottom = SonaraSpacing.massive,
        ),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
    ) {
        item {
            Column {
                Text(
                    text = "Sonara",
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalSonaraColors.current.textSecondary,
                )
                Text(
                    text = "Listen ambiently",
                    style = MaterialTheme.typography.displayMedium,
                    color = LocalSonaraColors.current.textPrimary,
                )
            }
        }

        item {
            SectionHeader(
                title = "Made for tonight",
                action = "See all",
                modifier = Modifier.padding(top = SonaraSpacing.sm),
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md)) {
                featured.forEachIndexed { index, track ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onPlayTrack(index) },
                        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                    ) {
                        ArtworkPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalSonaraColors.current.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Recently played",
                action = "History",
                modifier = Modifier.padding(top = SonaraSpacing.lg),
            )
        }

        items(recent.size) { index ->
            val track = recent[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onPlayTrack(index + featured.size) }
                    .padding(vertical = SonaraSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkPlaceholder(
                    modifier = Modifier.size(SonaraSpacing.huge),
                    shape = MaterialTheme.shapes.small,
                )
                Column {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalSonaraColors.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalSonaraColors.current.textSecondary,
                    )
                }
            }
        }
    }
}
