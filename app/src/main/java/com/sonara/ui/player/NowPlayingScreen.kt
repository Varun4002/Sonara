package com.sonara.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ambient.generatedAmbientPalette
import com.sonara.data.LibraryRepository
import com.sonara.playback.DemoCatalog
import com.sonara.playback.DemoTrack
import com.sonara.playback.NowPlayingState
import com.sonara.playback.PlayerConnection
import com.sonara.ui.components.AmbientBackground
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.SonaraTrackRow
import com.sonara.ui.designsystem.LocalBottomChromeHeight
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Sonara's signature screen: the song's environment fills the display (the
 * shell's overlay scaffold paints it) and the player scrolls over it.
 * Hierarchy: artwork → track → progress → controls; Lyrics, Up Next (the
 * real Media3 queue) and Related (the current album) are revealed below the
 * fold. Every section shows honest state — no fake content, no dead controls.
 */
@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    player: PlayerConnection,
    library: LibraryRepository,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
            // Top bar: collapse + queue. No app bar chrome; environment shows.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonaraSpacing.screenPadding, vertical = SonaraSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Collapse player",
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(SonaraSpacing.xxl)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCollapse,
                        ),
                )
                Icon(
                    imageVector = Icons.Outlined.QueueMusic,
                    contentDescription = "Open queue",
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(SonaraSpacing.xl)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenQueue,
                        ),
                )
            }

            PlayerContent(
                state = state,
                player = player,
                library = library,
                modifier = Modifier.weight(1f),
            )
    }
}

@Composable
private fun PlayerContent(
    state: NowPlayingState,
    player: PlayerConnection,
    library: LibraryRepository,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val haptics = LocalHapticFeedback.current
    val liked = library.isLiked(state.mediaId)
    val queue by player.queue.collectAsState()
    val shuffle by player.shuffleEnabled.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()

    // Optimistic seek position; authoritative on release.
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = SonaraSpacing.screenPadding,
            end = SonaraSpacing.screenPadding,
            bottom = LocalBottomChromeHeight.current + SonaraSpacing.xxl,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArtworkWithGlow(state = state)

                Spacer(modifier = Modifier.height(SonaraSpacing.xxl))

                // Track info; crossfades on track change.
                AnimatedContent(
                    targetState = state.mediaId,
                    transitionSpec = {
                        (fadeIn(tween(SonaraMotion.ArtworkTransition)))
                            .togetherWith(fadeOut(tween(SonaraMotion.ArtworkTransition)))
                    },
                    label = "now-playing-info",
                ) { mediaId ->
                    val track = mediaId?.let(DemoCatalog::trackById)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs),
                    ) {
                        Text(
                            text = track?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track?.artist.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SonaraSpacing.lg))

                FavoriteButton(liked = liked) {
                    state.mediaId?.let { id ->
                        library.toggleLike(id)
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                }

                Spacer(modifier = Modifier.height(SonaraSpacing.xl))

                ProgressSection(
                    state = state,
                    dragging = dragging,
                    dragPositionMs = dragPositionMs,
                    onValueChange = {
                        dragging = true
                        dragPositionMs = it * state.durationMs
                    },
                    onValueChangeFinished = {
                        player.seekTo(dragPositionMs.toLong())
                        dragging = false
                    },
                )

                Spacer(modifier = Modifier.height(SonaraSpacing.lg))

                PlaybackControls(
                    state = state,
                    player = player,
                    shuffleEnabled = shuffle,
                    repeatMode = repeatMode,
                )
            }
        }

        item(key = "lyrics-header") {
            SectionHeader(title = "Lyrics", modifier = Modifier.padding(top = SonaraSpacing.sectionGap))
        }
        item(key = "lyrics") {
            Text(
                text = "Lyrics aren't available for this song yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.padding(top = SonaraSpacing.xs),
            )
        }

        item(key = "upnext-header") {
            SectionHeader(
                title = "Up next",
                modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
            )
        }
        if (queue.isEmpty()) {
            item(key = "upnext-empty") {
                Text(
                    text = "Your queue is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = SonaraSpacing.xs),
                )
            }
        } else {
            items(queue.size, key = { i -> "queue-" + queue[i].mediaId }) { index ->
                val entry = queue[index]
                SonaraTrackRow(
                    mediaId = entry.mediaId,
                    title = entry.title,
                    subtitle = entry.artist,
                    isPlaying = entry.isCurrent,
                    onClick = { player.playQueueIndex(index) },
                )
            }
            item(key = "upnext-clear") {
                Text(
                    text = "Clear up next",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clickable(onClick = player::clearUpNext)
                        .padding(top = SonaraSpacing.sm, bottom = SonaraSpacing.xs),
                )
            }
        }

        val currentTrack = state.mediaId?.let(DemoCatalog::trackById)
        val related = currentTrack
            ?.let { DemoCatalog.tracksForAlbum(it.album).filterNot { t -> t.id == it.id } }
            .orEmpty()
        if (related.isNotEmpty()) {
            item(key = "related-header") {
                SectionHeader(
                    title = "More from ${currentTrack?.album}",
                    modifier = Modifier.padding(top = SonaraSpacing.sectionGap),
                )
            }
            items(related.size, key = { i -> "related-" + related[i].id }) { index ->
                val track = related[index]
                SonaraTrackRow(
                    mediaId = track.id,
                    title = track.title,
                    subtitle = track.artist,
                    isPlaying = false,
                    onClick = {
                        player.playTrack(DemoCatalog.tracks.indexOfFirst { it.id == track.id })
                    },
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(
    state: NowPlayingState,
    dragging: Boolean,
    dragPositionMs: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val colors = LocalSonaraColors.current
    val positionMs = if (dragging) dragPositionMs else state.positionMs.toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (state.durationMs > 0) {
                (positionMs / state.durationMs).coerceIn(0f, 1f)
            } else 0f,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = colors.textPrimary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = colors.glassBorder,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Playback position" },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimeLabel(formatTime(positionMs.toLong()))
            TimeLabel("-" + formatTime((state.durationMs - positionMs.toLong()).coerceAtLeast(0)))
        }
    }
}

@Composable
private fun PlaybackControls(
    state: NowPlayingState,
    player: PlayerConnection,
    shuffleEnabled: Boolean,
    repeatMode: Int,
) {
    val colors = LocalSonaraColors.current
    val repeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xxl),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous track",
                tint = colors.textPrimary,
                modifier = Modifier
                    .size(SonaraSpacing.xxl)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = player::seekToPrevious,
                    ),
            )
            // Primary control: soft glass circle, visually dominant.
            Box(
                modifier = Modifier
                    .size(SonaraSpacing.massive)
                    .clip(CircleShape)
                    .background(colors.textPrimary.copy(alpha = 0.14f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = player::togglePlayPause,
                    )
                    .semantics {
                        contentDescription = if (state.isPlaying) "Pause" else "Play"
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = state.isPlaying,
                    transitionSpec = {
                        (fadeIn(tween(SonaraMotion.Fast)) + scaleInSafe())
                            .togetherWith(fadeOut(tween(SonaraMotion.Fast)) + scaleOutSafe())
                    },
                    label = "np-play-pause",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(SonaraSpacing.xxxl),
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                tint = colors.textPrimary,
                modifier = Modifier
                    .size(SonaraSpacing.xxl)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = player::seekToNext,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(SonaraSpacing.lg))

        // Secondary modes — real Media3 shuffle/repeat, quiet when off.
        Row(
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else colors.textMuted,
                modifier = Modifier
                    .size(SonaraSpacing.xl)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = player::toggleShuffle,
                    ),
            )
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> "Repeat one"
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> "Repeat all"
                    else -> "Repeat off"
                },
                tint = if (repeatActive) MaterialTheme.colorScheme.primary else colors.textMuted,
                modifier = Modifier
                    .size(SonaraSpacing.xl)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = player::cycleRepeatMode,
                    ),
            )
        }
    }
}

private fun scaleInSafe() = androidx.compose.animation.scaleIn(
    initialScale = 0.7f,
    animationSpec = tween(SonaraMotion.Fast),
)

private fun scaleOutSafe() = androidx.compose.animation.scaleOut(
    targetScale = 0.7f,
    animationSpec = tween(SonaraMotion.Fast),
)

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = LocalSonaraColors.current.textSecondary,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ArtworkWithGlow(state: NowPlayingState, modifier: Modifier = Modifier) {
    val paletteGlow = remember(state.mediaId) {
        val p = generatedAmbientPalette(state.mediaId ?: "")
        listOf(p.highlight.copy(alpha = 0.45f), Color.Transparent)
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Environmental depth: soft glow derived from the track palette.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    Brush.radialGradient(
                        colors = paletteGlow,
                    ),
                ),
        )
        AnimatedContent(
            targetState = state.mediaId,
            transitionSpec = {
                fadeIn(tween(SonaraMotion.ArtworkTransition))
                    .togetherWith(fadeOut(tween(SonaraMotion.ArtworkTransition)))
            },
            label = "now-playing-artwork",
            modifier = Modifier.fillMaxWidth(0.78f),
        ) { mediaId ->
            SonaraArtwork(
                mediaId = mediaId,
                shape = SonaraShapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentDescriptionText = "Album artwork",
            )
        }
    }
}

@Composable
private fun FavoriteButton(liked: Boolean, onToggle: () -> Unit) {
    Icon(
        imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = if (liked) "Remove from liked songs" else "Add to liked songs",
        tint = if (liked) MaterialTheme.colorScheme.primary else LocalSonaraColors.current.textSecondary,
        modifier = Modifier
            .size(SonaraSpacing.xl)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
    )
}

@Composable
private fun TimeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFeatureSettings = "tnum",
        ),
        color = LocalSonaraColors.current.textSecondary,
    )
}

/** mm:ss formatting shared with the mini-player's state labels. */
internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
