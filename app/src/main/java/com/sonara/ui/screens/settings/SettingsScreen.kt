package com.sonara.ui.screens.settings

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sonara.ambient.AmbientVisualMode
import com.sonara.di.appContainer
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Settings — intentionally boring. Only options that actually change
 * behavior: ambient visual intensity and dynamic artwork colors. Playback and
 * audio sections stay out until the backend truly supports the toggles.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    val scope = rememberCoroutineScope()
    val container = androidx.compose.ui.platform.LocalContext.current.appContainer
    val snapshot by container.settingsSnapshot.collectAsState()

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
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                )
            }
        }

        item {
            SettingsSectionHeader("Appearance")
        }
        items(AmbientVisualMode.entries.size) { index ->
            val mode = AmbientVisualMode.entries[index]
            SettingRow(
                title = when (mode) {
                    AmbientVisualMode.On -> "Ambient visuals: On"
                    AmbientVisualMode.Reduced -> "Ambient visuals: Reduced"
                    AmbientVisualMode.Off -> "Ambient visuals: Off"
                },
                subtitle = when (mode) {
                    AmbientVisualMode.On -> "Full reactive environment"
                    AmbientVisualMode.Reduced -> "Calmer, fewer fields"
                    AmbientVisualMode.Off -> "Static neutral atmosphere"
                },
                leading = {
                    RadioButton(
                        selected = snapshot.ambientMode == mode,
                        onClick = { scope.launch { container.settings.setAmbientMode(mode) } },
                    )
                },
                onClick = { scope.launch { container.settings.setAmbientMode(mode) } },
            )
        }
        item {
            SettingRow(
                title = "Dynamic artwork colors",
                subtitle = "Let the current song drive the environment's palette",
                leading = {
                    Switch(
                        checked = snapshot.dynamicColors,
                        onCheckedChange = { scope.launch { container.settings.setDynamicColors(it) } },
                    )
                },
                onClick = { scope.launch { container.settings.setDynamicColors(!snapshot.dynamicColors) } },
            )
        }

        item {
            SettingsSectionHeader("About")
        }
        item {
            SettingRow(
                title = "Sonara",
                subtitle = "Version 0.1.0 · ambient playback stage",
                leading = null,
                onClick = null,
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = LocalSonaraColors.current.textSecondary,
        modifier = Modifier.padding(top = SonaraSpacing.sectionGap, bottom = SonaraSpacing.xs),
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    leading: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
) {
    val colors = LocalSonaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = SonaraSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        leading?.invoke()
    }
}
