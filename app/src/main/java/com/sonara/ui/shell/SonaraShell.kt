package com.sonara.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sonara.playback.PlayerConnection
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.player.SonaraMiniPlayer
import com.sonara.ui.screens.flow.FlowScreen
import com.sonara.ui.screens.home.HomeScreen
import com.sonara.ui.screens.library.LibraryScreen
import com.sonara.ui.screens.search.SearchScreen
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Sonara application shell: current screen, optional now-playing bar, and a
 * floating glass navigation bar. Tab state survives process death via
 * [rememberSaveable]; transitions use the centralized motion tokens.
 */
@Composable
fun SonaraShell(
    player: PlayerConnection,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(SonaraTab.Home) }
    val playback by player.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(tween(SonaraMotion.PageTransition, easing = SonaraMotion.EmphasizedEasing)))
                    .togetherWith(fadeOut(tween(SonaraMotion.PageTransition, easing = SonaraMotion.ExitEasing)))
            },
            label = "sonara-tab-content",
        ) { tab ->
            when (tab) {
                SonaraTab.Home -> HomeScreen(onPlayTrack = player::playTrack)
                SonaraTab.Search -> SearchScreen()
                SonaraTab.Library -> LibraryScreen()
                SonaraTab.Flow -> FlowScreen()
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = SonaraSpacing.xl)
                .navigationBarsPadding()
                .padding(bottom = SonaraSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xs),
        ) {
            SonaraMiniPlayer(
                state = playback,
                onTogglePlayPause = player::togglePlayPause,
                onSeekToNext = player::seekToNext,
            )
            SonaraBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
    }
}

@Composable
private fun SonaraBottomBar(
    selectedTab: SonaraTab,
    onTabSelected: (SonaraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current

    Row(
        modifier = modifier
            .sonaraGlass(shape = SonaraShapes.pill)
            .padding(horizontal = SonaraSpacing.md, vertical = SonaraSpacing.xs),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SonaraTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onTabSelected(tab) }
                    .padding(horizontal = SonaraSpacing.md, vertical = SonaraSpacing.xs),
            ) {
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else colors.textSecondary,
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                )
            }
        }
    }
}
