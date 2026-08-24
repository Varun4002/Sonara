package com.sonara.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonara.ui.components.ArtworkPlaceholder
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

private val moods = listOf("Focus", "Drift", "Night", "Rain", "Space", "Warmth")

/** Search tab placeholder. Catalog-backed search arrives in a later stage. */
@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SonaraSpacing.screenPadding,
            top = SonaraSpacing.xxl,
            end = SonaraSpacing.screenPadding,
            bottom = SonaraSpacing.massive,
        ),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xl)) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    color = LocalSonaraColors.current.textPrimary,
                )
                SearchFieldPlaceholder()
            }
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Browse by mood",
                style = MaterialTheme.typography.titleLarge,
                color = LocalSonaraColors.current.textPrimary,
            )
        }

        items(moods.size) { index ->
            Column {
                ArtworkPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = MaterialTheme.shapes.medium,
                )
                Text(
                    text = moods[index],
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalSonaraColors.current.textPrimary,
                    modifier = Modifier.padding(top = SonaraSpacing.xs),
                )
            }
        }
    }
}

/** Non-interactive stand-in for the real search field of a later stage. */
@Composable
private fun SearchFieldPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .sonaraGlass(shape = SonaraShapes.pill)
            .padding(horizontal = SonaraSpacing.lg),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = LocalSonaraColors.current.textMuted,
        )
    }
}
