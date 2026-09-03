package com.sonara.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.sonara.auth.AuthState
import com.sonara.ui.components.AmbientBackground
import com.sonara.ambient.AmbientVisualEngine
import com.sonara.ui.designsystem.SonaraMotion
import com.sonara.ui.designsystem.SonaraShapes
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

@Composable
fun OnboardingScreen(
    engine: AmbientVisualEngine,
    authState: AuthState,
    googleConfigured: Boolean,
    onSignInGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(SonaraMotion.Slow, easing = SonaraMotion.EmphasizedEasing))
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackground(engine = engine, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = SonaraSpacing.screenPadding)
                .alpha(entrance.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(SonaraSpacing.massive))

            Text(
                text = "Sonara",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to Sonara",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Your music, your atmosphere.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = SonaraSpacing.sm),
                )
            }

            Box(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.md),
            ) {
                when (val state = authState) {
                    is AuthState.Authenticating -> {
                        Box(
                            modifier = Modifier
                                .height(SonaraSpacing.huge)
                                .semantics { contentDescription = "Connecting" },
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SonaraSpacing.xl),
                            )
                        }
                    }

                    is AuthState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    else -> Unit
                }

                StartButton(
                    enabled = authState is AuthState.SignedOut || authState is AuthState.Error,
                    onClick = onSignInGoogle,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Connect with your Google account to access your YouTube Music library.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = SonaraSpacing.xl),
                )
            }
        }
    }
}

@Composable
private fun StartButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonaraColors.current
    Box(
        modifier = modifier
            .clip(SonaraShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = SonaraSpacing.xl, vertical = SonaraSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Start",
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
        )
    }
}
