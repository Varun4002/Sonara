package com.sonara.ui.screens.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonara.ui.components.sonaraGlass
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors
import com.sonara.ui.theme.SonaraPrimary

/**
 * Flow tab — Sonara's signature endless ambient mix. The generative engine is
 * a later stage; this screen establishes its hero layout.
 */
@Composable
fun FlowScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SonaraSpacing.screenPadding)
            .padding(top = SonaraSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SonaraSpacing.xxs),
        ) {
            Text(
                text = "Flow",
                style = MaterialTheme.typography.displayMedium,
                color = LocalSonaraColors.current.textPrimary,
            )
            Text(
                text = "An endless stream, tuned to this moment.",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalSonaraColors.current.textSecondary,
            )
        }

        FlowOrb(modifier = Modifier.weight(1f))

        Text(
            text = "Flow blends your library into a seamless ambient journey. It never repeats, it never ends.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSonaraColors.current.textSecondary,
            modifier = Modifier.padding(bottom = SonaraSpacing.massive),
        )
    }
}

@Composable
private fun FlowOrb(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SonaraSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 220.dp)
                .fillMaxWidth()
                .sonaraGlass(shape = SonaraShapes.large)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(SonaraPrimary.copy(alpha = 0.28f), Color.Transparent),
                    ),
                    shape = SonaraShapes.large,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Start Flow",
                style = MaterialTheme.typography.headlineMedium,
                color = LocalSonaraColors.current.textPrimary,
            )
        }
    }
}
