package com.sonara.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sonara.ambient.AmbientEngine
import com.sonara.data.LibraryRepository
import com.sonara.playback.PlayerConnection
import com.sonara.ui.components.AmbientBackground
import com.sonara.ui.components.SonaraLiquidGlass
import com.sonara.ui.designsystem.LocalBottomChromeHeight
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
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

/**
 * Sonara application shell: the continuous ambient environment behind a tab
 * host, a small overlay stack, and the floating bottom system — mini-player
 * above, navigation island below — both in the same liquid glass. The shell
 * owns the player, the navigation, and the dynamic accent; screens only
 * render content.
 */
@Composable
fun SonaraShell(
    player: PlayerConnection,
    library: LibraryRepository,
    ambient: AmbientEngine,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(SonaraTab.Home) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    val playback by player.state.collectAsState()
    val ambientState by ambient.state.collectAsState()

    // Single injection point: glass roles and the Material accent follow the
    // current song's palette. Purple appears only when the music is purple.
    val resolvedPalette = ambientState.resolved()
    val dynamicColors = remember(resolvedPalette) {
        val tint = resolvedPalette.glassTint()
        SonaraColors(glassSurface = tint.surface, glassBorder = tint.border)
    }
    val dynamicScheme = remember(resolvedPalette) {
        SonaraColorScheme.copy(primary = resolvedPalette.accent)
    }
    val colors = LocalSonaraColors.current

    BackHandler(enabled = overlay != null) { overlay = null }

    CompositionLocalProvider(LocalSonaraColors provides dynamicColors) {
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
                                library = library,
                            )
                            SonaraTab.Search -> SearchScreen(
                                onPlayTrack = player::playTrack,
                                onOpenAlbum = { overlay = "album:$it" },
                            )
                            SonaraTab.Library -> LibraryScreen(
                                library = library,
                                onPlayTrack = player::playTrack,
                                onOpenLiked = { overlay = "liked" },
                                onOpenAlbum = { overlay = "album:$it" },
                                onOpenArtist = { overlay = "artist" },
                                onOpenSettings = { overlay = "settings" },
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
                    when {
                        current == null -> Box(Modifier.fillMaxSize())
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

                // Global floating bottom system: mini-player above, navigation
                // island below, one deliberate gap between them. Both share the
                // liquid glass and the ambient tint. Hidden only while an
                // overlay owns the screen.
                if (overlay == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { chromeHeightPx = it.height }
                            .padding(horizontal = SonaraSpacing.xl)
                            .navigationBarsPadding()
                            .padding(bottom = SonaraSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
                    ) {
                        SonaraMiniPlayer(
                            state = playback,
                            engine = ambient.engine,
                            onTogglePlayPause = player::togglePlayPause,
                            onSeekToNext = player::seekToNext,
                            onExpand = { overlay = "nowplaying" },
                        )
                        SonaraBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            engine = ambient.engine,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating navigation island: liquid glass, equal-width destinations, and a
 * soft tonal selection container that glides between items. The accent comes
 * from the ambient palette — never a permanent purple.
 */
@Composable
private fun SonaraBottomBar(
    selectedTab: SonaraTab,
    onTabSelected: (SonaraTab) -> Unit,
    engine: com.sonara.ambient.AmbientVisualEngine,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val selectedIndex = selectedTab.ordinal

    SonaraLiquidGlass(
        engine = engine,
        shape = SonaraShapes.extraLarge,
        modifier = modifier
            .semantics { contentDescription = "Main navigation" },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(SonaraSpacing.xs)
                .selectableGroup(),
        ) {
            val itemWidth = maxWidth / SonaraTab.entries.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = tween(SonaraMotion.Normal, easing = SonaraMotion.StandardEasing),
                label = "nav-selection-indicator",
            )

            // Soft tonal container behind the selected destination.
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(SonaraSpacing.navBarHeight)
                    .padding(SonaraSpacing.xxs)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        SonaraShapes.medium,
                    ),
            )

            Row(modifier = Modifier.height(SonaraSpacing.navBarHeight)) {
                SonaraTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onTabSelected(tab) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else colors.textSecondary,
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier.padding(top = SonaraSpacing.xxs),
                        )
                    }
                }
            }
        }
    }
}
