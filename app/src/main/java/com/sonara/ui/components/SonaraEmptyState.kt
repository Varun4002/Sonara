package com.sonara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sonara.ui.designsystem.SonaraSpacing
import com.sonara.ui.theme.LocalSonaraColors

/**
 * Quiet editorial empty state: what is empty, why/what now, optional action
 * slot. No decorative graphics — empty space stays intentional.
 */
@Composable
fun SonaraEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(
            horizontal = SonaraSpacing.xxl,
            vertical = SonaraSpacing.massive,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LocalSonaraColors.current.textPrimary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSonaraColors.current.textSecondary,
        )
        action?.let {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(SonaraSpacing.xxs))
            it()
        }
    }
}
