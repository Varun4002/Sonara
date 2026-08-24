package com.sonara.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.ui.components.ArtworkPlaceholder
import com.sonara.ui.components.SectionHeader
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

private val sections = listOf("Playlists", "Albums", "Artists")

/** Library tab placeholder. Persistence-backed content arrives later. */
@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
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
            Text(
                text = "Library",
                style = MaterialTheme.typography.displayMedium,
                color = LocalSonaraColors.current.textPrimary,
            )
        }

        sections.forEach { section ->
            item(key = section) {
                SectionHeader(
                    title = section,
                    action = "See all",
                    modifier = Modifier.padding(top = SonaraSpacing.lg),
                )
            }
            items(2) { index ->
                Row(
                    modifier = Modifier.padding(vertical = SonaraSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkPlaceholder(
                        modifier = Modifier.size(SonaraSpacing.huge),
                        shape = MaterialTheme.shapes.small,
                    )
                    Column {
                        Text(
                            text = "$section entry ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalSonaraColors.current.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Placeholder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalSonaraColors.current.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
