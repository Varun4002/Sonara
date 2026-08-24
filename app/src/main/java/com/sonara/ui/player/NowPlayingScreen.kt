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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.sonara.ambient.generatedAmbientPalette
import com.sonara.data.LibraryRepository
import com.sonara.playback.DemoCatalog
import com.sonara.playback.NowPlayingState
import com.sonara.playback.PlayerConnection
import com.sonara.ui.components.SonaraArtwork
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Sonara's signature screen: the song's environment fills the display and the
 * UI floats above it. Artwork is the anchor; a palette-derived glow provides
 * environmental depth instead of a hard shadow. Seek is optimistic — the
 * visual position follows the drag, and the player becomes authoritative on
 * release.
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
    val haptics = LocalHapticFeedback.current
    val liked = library.isLiked(state.mediaId)

    // Optimistic seek position; null while not dragging.
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = SonaraSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: collapse + queue. No giant app bar; background stays visible.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SonaraSpacing.sm),
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

        Spacer(modifier = Modifier.padding(SonaraSpacing.xxxl))

        ArtworkWithGlow(state = state)

        Spacer(modifier = Modifier.padding(SonaraSpacing.xxl))

        // Track info + favorite. Title dominant; artist secondary.
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
                    maxLines = 1,
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

        Spacer(modifier = Modifier.size(SonaraSpacing.xxl))

        FavoriteButton(liked = liked) {
            state.mediaId?.let { id ->
                library.toggleLike(id)
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        }

        Spacer(modifier = Modifier.padding(SonaraSpacing.lg))

        // Progress: visual follows the drag; audio is authoritative on release.
        val positionMs = if (dragging) dragPositionMs else state.positionMs.toFloat()
        Slider(
            value = if (state.durationMs > 0) {
                (positionMs / state.durationMs).coerceIn(0f, 1f)
            } else 0f,
            onValueChange = {
                dragging = true
                dragPositionMs = it * state.durationMs
            },
            onValueChangeFinished = {
                player.seekTo(dragPositionMs.toLong())
                dragging = false
            },
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

        Spacer(modifier = Modifier.padding(SonaraSpacing.lg))

        // Primary controls: previous / play / next, play visually dominant.
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
            Box(
                modifier = Modifier
                    .size(SonaraSpacing.massive)
                    .clip(SonaraShapes.pill)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
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
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF0B0B0F),
                    modifier = Modifier.size(SonaraSpacing.xxxl),
                )
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

        Spacer(modifier = Modifier.padding(SonaraSpacing.lg))

        // Secondary controls, deliberately quiet.
        Row(
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = "Shuffle",
                tint = colors.textMuted,
                modifier = Modifier.size(SonaraSpacing.xl),
            )
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = "Repeat",
                tint = colors.textMuted,
                modifier = Modifier.size(SonaraSpacing.xl),
            )
        }
    }
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
                shape = SonaraShapes.large,
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
