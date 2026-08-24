package com.sonara.ui.screens.liked

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sonara.data.LibraryRepository
import com.sonara.playback.DemoCatalog
import com.sonara.ui.components.SonaraEmptyState
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Liked Songs collection. Real session state: hearts added in Now Playing
 * appear here immediately and are playable.
 */
@Composable
fun LikedScreen(
    library: LibraryRepository,
    onPlayTrack: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val liked by library.likedIds.collectAsState()
    val tracks = liked.mapNotNull(DemoCatalog::trackById)

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
                Column {
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                SonaraEmptyState(
                    title = "Nothing liked yet",
                    body = "Tap the heart on a playing track and it will live here.",
                    modifier = Modifier.padding(top = SonaraSpacing.xxxl),
                )
            }
        } else {
            items(tracks.size) { index ->
                val track = tracks[index]
                SonaraTrackRow(
                    mediaId = track.id,
                    title = track.title,
                    subtitle = track.artist,
                    onClick = {
                        onPlayTrack(DemoCatalog.tracks.indexOfFirst { it.id == track.id })
                    },
                )
            }
        }
    }
}
