package com.sonara.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.data.LibraryRepository
import com.sonara.music.Album
import com.sonara.music.Artist
import com.sonara.music.HomeFeed
import com.sonara.music.HomeItem
import com.sonara.music.HomeSectionType
import com.sonara.music.HomeScreenState
import com.sonara.music.MusicRepository
import com.sonara.music.Track
import com.sonara.music.timeBasedGreeting
import com.sonara.playback.DemoCatalog
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SectionHeader
import com.sonara.ui.components.SonaraEmptyState
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.components.sonaraPressScale
import com.sonara.ui.designsystem.LocalBottomChromeHeight
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Home — the primary discovery surface. A calm, editorial top region with
 * generous breathing room, greeting, and provider-backed sections.
 *
 * States:
 *   Loading — subtle skeleton placeholders, no blank screen
 *   Loaded  — real sections from [MusicRepository]
 *   Error   — message + retry; cached data shown when available
 *   Empty   — quiet "nothing here yet"
 *
 * The ambient background stays behind; nothing scrolls it.
 */
@Composable
fun HomeScreen(
    onPlayTrack: (Int) -> Unit,
    onPlayTrackById: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    library: LibraryRepository,
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val homeState by musicRepo.homeState.collectAsState()

    // Trigger initial load.
    LaunchedEffect(musicRepo) {
        musicRepo.refreshHome()
    }

    when (val state = homeState) {
        is HomeScreenState.Loading -> HomeLoading(modifier = modifier)
        is HomeScreenState.Error -> HomeError(
            message = state.message,
            onRetry = { /* LaunchedEffect will re-trigger */ },
            modifier = modifier,
        )
        is HomeScreenState.Empty -> HomeEmpty(modifier = modifier)
        is HomeScreenState.Loaded -> HomeContent(
            feed = state.feed,
            onPlayTrack = onPlayTrack,
            onPlayTrackById = onPlayTrackById,
            onPlayTracks = onPlayTracks,
            library = library,
            musicRepo = musicRepo,
            modifier = modifier,
        )
    }
}

// ---------------------------------------------------------------------------
// Loaded content
// ---------------------------------------------------------------------------

@Composable
private fun HomeContent(
    feed: HomeFeed,
    onPlayTrack: (Int) -> Unit,
    onPlayTrackById: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    library: LibraryRepository,
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val history by library.history.collectAsState()

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
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
    ) {
        // -- Greeting ----------------------------------------------------
        item("greeting") {
            Column(
                modifier = Modifier.padding(top = SonaraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs),
            ) {
                Text(
                    text = timeBasedGreeting(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = "What would you like to listen to?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
        }

        // -- Provider sections -------------------------------------------
        feed.sections.forEach { section ->
            when (section.type) {
                HomeSectionType.QUICK_PICKS -> {
                    val tracks = section.items.filterIsInstance<HomeItem.TrackItem>().map { it.track }
                    if (tracks.isNotEmpty()) {
                        item("section_${section.id}_title") {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
                            )
                        }
                        item("section_${section.id}_row") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                            ) {
                                items(
                                    count = tracks.size,
                                    key = { tracks[it].id },
                                ) { index ->
                                    QuickPickCard(
                                        track = tracks[index],
                                        onClick = { onPlayTracks(tracks, index) },
                                    )
                                }
                            }
                        }
                    }
                }
                HomeSectionType.CONTINUE_LISTENING -> {
                    val tracks = section.items.filterIsInstance<HomeItem.TrackItem>().map { it.track }
                    if (tracks.isNotEmpty()) {
                        item("section_${section.id}_title") {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
                            )
                        }
                        items(
                            count = tracks.size,
                            key = { "cl_${tracks[it].id}" },
                        ) { index ->
                            SonaraTrackRow(
                                mediaId = tracks[index].artworkMediaId ?: tracks[index].id,
                                title = tracks[index].title,
                                subtitle = tracks[index].artist,
                                isPlaying = false,
                                onClick = { onPlayTracks(tracks, index) },
                            )
                        }
                    }
                }
                HomeSectionType.ALBUMS -> {
                    val albums = section.items.filterIsInstance<HomeItem.AlbumItem>().map { it.album }
                    if (albums.isNotEmpty()) {
                        item("section_${section.id}_title") {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
                            )
                        }
                        items(
                            count = albums.size,
                            key = { albums[it].id },
                        ) { index ->
                            AlbumRow(albums[index])
                        }
                    }
                }
                HomeSectionType.ARTISTS -> {
                    val artists = section.items.filterIsInstance<HomeItem.ArtistItem>().map { it.artist }
                    if (artists.isNotEmpty()) {
                        item("section_${section.id}_title") {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
                            )
                        }
                        items(
                            count = artists.size,
                            key = { artists[it].id },
                        ) { index ->
                            ArtistRow(artists[index])
                        }
                    }
                }
                else -> {
                    // RECOMMENDED and any untyped section: render whatever
                    // item types the provider returned (tracks, playlists,
                    // albums) so real content never silently disappears.
                    val items = section.items
                    if (items.isNotEmpty()) {
                        item("section_${section.id}_title") {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
                            )
                        }
                        items(
                            count = items.size,
                            key = { index ->
                                val item = items[index]
                                when (item) {
                                    is HomeItem.TrackItem -> "generic_track_${item.track.id}"
                                    is HomeItem.PlaylistItem -> "generic_playlist_${item.playlist.id}"
                                    is HomeItem.AlbumItem -> "generic_album_${item.album.id}"
                                    is HomeItem.ArtistItem -> "generic_artist_${item.artist.name}"
                                }
                            },
                        ) { index ->
                            when (val item = items[index]) {
                                is HomeItem.TrackItem -> {
                                    val tracks = items.filterIsInstance<HomeItem.TrackItem>().map { it.track }
                                    val trackStart = items.take(index).count { it is HomeItem.TrackItem }
                                    SonaraTrackRow(
                                        mediaId = item.track.artworkMediaId ?: item.track.id,
                                        model = item.track.artworkUrl,
                                        title = item.track.title,
                                        subtitle = item.track.artist,
                                        onClick = { onPlayTracks(tracks, trackStart) },
                                    )
                                }
                                is HomeItem.PlaylistItem -> MediaRow(
                                    mediaId = item.playlist.id,
                                    title = item.playlist.title,
                                    subtitle = "Playlist",
                                    artworkUrl = item.playlist.artworkUrl,
                                )
                                is HomeItem.AlbumItem -> MediaRow(
                                    mediaId = item.album.artworkMediaId ?: item.album.id,
                                    title = item.album.title,
                                    subtitle = item.album.artist,
                                    artworkUrl = item.album.artworkUrl,
                                )
                                is HomeItem.ArtistItem -> MediaRow(
                                    mediaId = item.artist.trackIds.firstOrNull() ?: item.artist.id,
                                    title = item.artist.name,
                                    subtitle = "Artist",
                                    artworkUrl = item.artist.artworkUrl,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Card composables
// ---------------------------------------------------------------------------

@Composable
private fun QuickPickCard(
    track: Track,
    onClick: () -> Unit,
) {
    val colors = LocalSonaraColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(160.dp)
            .sonaraPressScale(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
    ) {
        SonaraArtwork(
            mediaId = track.artworkMediaId ?: track.id,
            shape = SonaraShapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentDescriptionText = "Play ${track.title}",
        )
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Simple content row for home-feed items without a dedicated detail flow yet. */
@Composable
private fun MediaRow(
    mediaId: String,
    title: String,
    subtitle: String,
    artworkUrl: String? = null,
) {
    val colors = LocalSonaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonaraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SonaraArtwork(
            mediaId = mediaId,
            model = artworkUrl,
            shape = SonaraShapes.small,
            modifier = Modifier.size(SonaraSpacing.huge),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AlbumRow(album: Album) {    val colors = LocalSonaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonaraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SonaraArtwork(
            mediaId = album.artworkMediaId ?: album.id,
            shape = SonaraShapes.small,
            modifier = Modifier.size(SonaraSpacing.huge),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    album.year?.let { append("$it · ") }
                    append("${album.trackIds.size} tracks")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist) {
    val colors = LocalSonaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonaraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SonaraArtwork(
            mediaId = artist.trackIds.firstOrNull() ?: artist.id,
            shape = SonaraShapes.pill,
            modifier = Modifier.size(SonaraSpacing.huge),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${artist.trackIds.size} tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Loading / Error / Empty states
// ---------------------------------------------------------------------------

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    val colors = LocalSonaraColors.current
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
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
    ) {
        // Greeting placeholder.
        item {
            Column(
                modifier = Modifier.padding(top = SonaraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs),
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth(0.5f)
                        .shimmerPlaceholder(),
                )
                Spacer(Modifier.height(SonaraSpacing.xs))
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(0.7f)
                        .shimmerPlaceholder(),
                )
            }
        }

        // Section placeholder: title + row of cards.
        item {
            Spacer(Modifier.height(SonaraSpacing.sectionGap))
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .fillMaxWidth(0.3f)
                    .shimmerPlaceholder(),
            )
            Spacer(Modifier.height(SonaraSpacing.md))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
            ) {
                items(4) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .shimmerPlaceholder(),
                        )
                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .width(120.dp)
                                .shimmerPlaceholder(),
                        )
                    }
                }
            }
        }

        // Track list placeholder.
        item {
            Spacer(Modifier.height(SonaraSpacing.sectionGap))
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .fillMaxWidth(0.3f)
                    .shimmerPlaceholder(),
            )
            Spacer(Modifier.height(SonaraSpacing.md))
        }
        items(3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(SonaraSpacing.huge)
                        .shimmerPlaceholder(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs)) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.5f)
                            .shimmerPlaceholder(),
                    )
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.35f)
                            .shimmerPlaceholder(),
                    )
                }
            }
        }
    }
}

/** Minimal skeleton placeholder — translucent elevated surface. */
@Composable
private fun Modifier.shimmerPlaceholder(alpha: Float = 0.4f): Modifier {
    val colors = LocalSonaraColors.current
    return this.then(
        Modifier.background(colors.elevatedSurface.copy(alpha = alpha)),
    )
}

@Composable
private fun HomeError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        SonaraEmptyState(
            title = "Couldn't load your music.",
            body = message,
            action = {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Try again",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    }
}

@Composable
private fun HomeEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        SonaraEmptyState(
            title = "Nothing here yet.",
            body = "Music will appear here once your provider delivers content.",
        )
    }
}
