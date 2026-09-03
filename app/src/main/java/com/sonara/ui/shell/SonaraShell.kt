package com.sonara.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.sonara.ambient.AmbientEngine
import com.sonara.data.LibraryRepository
import com.sonara.music.MusicRepository
import com.sonara.playback.DemoCatalog
import com.sonara.playback.PlayerConnection
import kotlin.math.roundToInt
import com.sonara.ui.components.AmbientBackground
import com.sonara.ui.designsystem.LocalBottomChromeHeight
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.material.LensRect
import com.sonara.ui.material.LiquidGlassDimensions
import com.sonara.ui.material.LiquidGlassSurface
import com.sonara.ui.material.LiquidGlassTokens
import com.sonara.ui.material.LocalAmbientEngine
import com.sonara.ui.material.LocalGlassTint
import com.sonara.ui.player.NowPlayingScreen
import com.sonara.ui.player.SonaraMiniPlayer
import com.sonara.ui.screens.album.AlbumScreen
import com.sonara.ui.screens.artist.ArtistScreen
import com.sonara.ui.screens.flow.FlowScreen
import com.sonara.ui.screens.home.HomeScreen
import com.sonara.ui.screens.library.LibraryScreen
import com.sonara.ui.screens.liked.LikedScreen
import com.sonara.ui.screens.queue.QueueScreen
import com.sonara.ui.screens.search.SearchScreen
import com.sonara.ui.screens.settings.SettingsScreen
import com.sonara.ui.theme.LocalSonaraColors
import com.sonara.ui.theme.SonaraColorScheme
import com.sonara.ui.theme.SonaraColors
import com.sonara.ui.theme.glassTint

/**
 * Sonara application shell: the continuous ambient environment behind a tab
 * host, an overlay stack, and the floating bottom system — mini-player above,
 * navigation below — as one liquid-glass composition. The shell owns the
 * player, navigation, and the dynamic accent; screens only render content.
 */
@Composable
fun SonaraShell(
    player: PlayerConnection,
    library: LibraryRepository,
    ambient: AmbientEngine,
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(SonaraTab.Home) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    val playback by player.state.collectAsState()
    val ambientState by ambient.state.collectAsState()

    // Single injection point: glass roles and the Material accent follow the
    // current song's palette. Purple appears only when the music is purple.
    val resolvedPalette = ambientState.resolved()
    val tint = remember(resolvedPalette) { resolvedPalette.glassTint() }
    val dynamicColors = remember(resolvedPalette) {
        SonaraColors(glassSurface = tint.surface, glassBorder = tint.border)
    }
    val dynamicScheme = remember(resolvedPalette) {
        SonaraColorScheme.copy(primary = resolvedPalette.accent)
    }

    BackHandler(enabled = overlay != null) { overlay = null }

    CompositionLocalProvider(
        LocalSonaraColors provides dynamicColors,
        LocalAmbientEngine provides ambient.engine,
        LocalGlassTint provides tint,
    ) {
        MaterialTheme(colorScheme = dynamicScheme) {
            // Measured height of the floating bottom system; screens pad their
            // content with it so nothing hides underneath.
            var chromeHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val chromeHeight = with(density) { chromeHeightPx.toDp() }

            Box(modifier = modifier.fillMaxSize()) {
                AmbientBackground(
                    engine = ambient.engine,
                    modifier = Modifier.fillMaxSize(),
                )

                CompositionLocalProvider(LocalBottomChromeHeight provides chromeHeight) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(SonaraMotion.PageTransition, easing = SonaraMotion.EmphasizedEasing)))
                                .togetherWith(fadeOut(tween(SonaraMotion.PageTransition, easing = SonaraMotion.ExitEasing)))
                        },
                        label = "sonara-tab-content",
                    ) { tab ->
                        when (tab) {
                            SonaraTab.Home -> HomeScreen(
                                onPlayTrack = player::playTrack,
                                onPlayTrackById = { trackId ->
                                    val idx = DemoCatalog.tracks.indexOfFirst { it.id == trackId }
                                    if (idx >= 0) player.playTrack(idx)
                                },
                                onPlayTracks = { tracks, index ->
                                    player.playTracks(tracks, index, musicRepo::resolvePlayback)
                                },
                                library = library,
                                musicRepo = musicRepo,
                            )
                            SonaraTab.Search -> SearchScreen(
                                onPlayTrack = player::playTrack,
                                onOpenAlbum = { overlay = "album:$it" },
                                musicRepo = musicRepo,
                                onPlayTracks = { tracks, index ->
                                    player.playTracks(tracks, index, musicRepo::resolvePlayback)
                                },
                            )
                            SonaraTab.Library -> LibraryScreen(
                                library = library,
                                onPlayTrack = player::playTrack,
                                onOpenLiked = { overlay = "liked" },
                                onOpenAlbum = { overlay = "album:$it" },
                                onOpenArtist = { overlay = "artist" },
                                onOpenSettings = { overlay = "settings" },
                                musicRepo = musicRepo,
                            )
                            SonaraTab.Flow -> FlowScreen(
                                player = player,
                                onOpenNowPlaying = { overlay = "nowplaying" },
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = overlay,
                    transitionSpec = {
                        val entering = when {
                            targetState == "nowplaying" ->
                                slideInVertically(tween(SonaraMotion.PlayerExpand, easing = SonaraMotion.EmphasizedEasing)) { it }
                            targetState != null ->
                                slideInHorizontally(tween(SonaraMotion.ScreenPush, easing = SonaraMotion.EmphasizedEasing)) { it }
                            else -> fadeIn(tween(SonaraMotion.Normal))
                        }
                        val exiting = when {
                            initialState == "nowplaying" ->
                                slideOutVertically(tween(SonaraMotion.PlayerExpand, easing = SonaraMotion.ExitEasing)) { it }
                            initialState != null ->
                                slideOutHorizontally(tween(SonaraMotion.ScreenPush, easing = SonaraMotion.ExitEasing)) { it }
                            else -> fadeOut(tween(SonaraMotion.Normal))
                        }
                        entering.togetherWith(exiting)
                    },
                    label = "sonara-overlay",
                ) { current ->
                    // Every overlay owns the full environment — opaque ambient
                    // base plus fields — so tab content never bleeds through.
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (current != null) {
                            AmbientBackground(
                                engine = ambient.engine,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                        when {
                            current == null -> Unit
                            current == "nowplaying" -> NowPlayingScreen(
                                state = playback,
                                player = player,
                                library = library,
                                onCollapse = { overlay = null },
                                onOpenQueue = { overlay = "queue" },
                                modifier = Modifier.fillMaxSize(),
                            )
                            current == "queue" -> QueueScreen(
                                player = player,
                                onBack = { overlay = null },
                            )
                            current?.startsWith("album:") == true -> AlbumScreen(
                                album = current.removePrefix("album:"),
                                onPlayTrack = player::playTrack,
                                onBack = { overlay = null },
                            )
                            current == "artist" -> ArtistScreen(
                                onPlayTrack = player::playTrack,
                                onOpenAlbum = { overlay = "album:$it" },
                                onBack = { overlay = null },
                            )
                            current == "liked" -> LikedScreen(
                                library = library,
                                onPlayTrack = player::playTrack,
                                onBack = { overlay = null },
                            )
                            current == "settings" -> SettingsScreen(
                                onBack = { overlay = null },
                            )
                        }
                    }
                }

                // Global floating bottom system: one liquid-glass composition —
                // mini-player above, navigation below, a deliberate gap between
                // them. Hidden only while an overlay owns the screen.
                if (overlay == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { chromeHeightPx = it.height }
                            .padding(horizontal = LiquidGlassDimensions.navigationHorizontalMargin)
                            .navigationBarsPadding()
                            .padding(bottom = LiquidGlassDimensions.navigationBottomInset),
                        verticalArrangement = Arrangement.spacedBy(LiquidGlassDimensions.bottomGap),
                    ) {
                        SonaraMiniPlayer(
                            state = playback,
                            onTogglePlayPause = player::togglePlayPause,
                            onSeekToNext = player::seekToNext,
                            onExpand = { overlay = "nowplaying" },
                        )
                        NavigationGlass(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The signature surface: one piece of liquid glass floating above the living
 * environment. Equal-width destination slots; the selected destination is an
 * optical lens inside the same material — brighter, more refractive, its own
 * specular rim — that physically glides between slots on a heavy spring.
 */
@Composable
private fun NavigationGlass(
    selectedTab: SonaraTab,
    onTabSelected: (SonaraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(LiquidGlassDimensions.navigationHeight),
    ) {
        val navHeight = LiquidGlassDimensions.navigationHeight
        val navRadius = LiquidGlassDimensions.navigationRadius
        val slotWidth = maxWidth / SonaraTab.entries.size
        val lensInset = LiquidGlassDimensions.lensInset

        val density = LocalDensity.current
        val glassWidthPx = with(density) { maxWidth.toPx() }
        val slotWidthPx = glassWidthPx / SonaraTab.entries.size

        // Base lens center for the current selected tab (glass-centered px).
        val baseCenterX = with(density) {
            (slotWidth * selectedTab.ordinal + slotWidth / 2).toPx() - glassWidthPx / 2
        }

        // Swipe-drag state: follows the finger during horizontal drag.
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        // Lens X: snaps during drag, springs when idle.
        val lensCenterX by animateFloatAsState(
            targetValue = if (isDragging) baseCenterX + dragOffsetPx else baseCenterX,
            animationSpec = if (isDragging) snap() else SonaraMotion.LensSpring,
            label = "nav-lens-x",
        )

        val lensHalfWidth = with(density) { (slotWidth - lensInset * 2).toPx() / 2 }
        val lensHalfHeight = with(density) { (navHeight - lensInset * 2).toPx() / 2 }
        val lensRadius = with(density) { (navRadius - lensInset / 2).toPx() }

        LiquidGlassSurface(
            shape = com.sonara.ui.designsystem.SonaraShapes.glassNavigation,
            cornerRadius = navRadius,
            intensity = LiquidGlassTokens.Standard,
            lens = LensRect(
                centerX = lensCenterX,
                centerY = 0f,
                halfWidth = lensHalfWidth,
                halfHeight = lensHalfHeight,
                cornerRadius = lensRadius,
            ),
            accentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragOffsetPx = 0f
                        },
                        onDragEnd = {
                            // Snap to the nearest tab.
                            val finalPx = baseCenterX + dragOffsetPx + glassWidthPx / 2
                            val index = (finalPx / slotWidthPx).roundToInt()
                                .coerceIn(0, SonaraTab.entries.lastIndex)
                            onTabSelected(SonaraTab.entries[index])
                            isDragging = false
                            dragOffsetPx = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetPx = 0f
                        },
                        onHorizontalDrag = { _, delta -> dragOffsetPx += delta },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navHeight)
                    .selectableGroup(),
            ) {
                SonaraTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (pressed) 0.97f else 1f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
                        label = "nav-press-scale",
                    )
                    val selectScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                        label = "nav-select-scale",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onTabSelected(tab) },
                            )
                            .graphicsLayer {
                                scaleX = pressScale * selectScale
                                scaleY = pressScale * selectScale
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = null,
                            tint = if (selected) Color.White else colors.textSecondary,
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else colors.textSecondary,
                            modifier = Modifier.padding(top = SonaraSpacing.xxs),
                        )
                    }
                }
            }
        }
    }
}
