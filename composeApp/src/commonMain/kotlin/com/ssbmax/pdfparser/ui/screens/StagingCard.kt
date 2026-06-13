package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun StagingCard(
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsRow(
            icon = "🌱",
            title = AppStrings.settingsStagingSeedBtn,
            subtitle = AppStrings.settingsStagingDesc,
            onClick = onSeedClick,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "🗑️",
            title = AppStrings.settingsStagingClearBtn,
            onClick = onClearClick,
        )
    }
}
