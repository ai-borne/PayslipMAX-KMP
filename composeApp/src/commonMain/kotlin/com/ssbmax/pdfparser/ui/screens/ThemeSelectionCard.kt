@file:OptIn(ExperimentalMaterial3Api::class)

package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun ThemeSelectionCard(
    currentTheme: String,
    onThemeSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val currentThemeLabel =
        when (currentTheme) {
            "light" -> AppStrings.settingsThemeLight
            "dark" -> AppStrings.settingsThemeDark
            else -> AppStrings.settingsThemeSystem
        }

    SettingsRow(
        icon = "🎨",
        title = AppStrings.settingsRowThemeLabel,
        subtitle = currentThemeLabel,
        onClick = { showSheet = true },
        modifier = modifier,
    )

    if (showSheet) {
        ThemeSelectionBottomSheet(
            currentTheme = currentTheme,
            onThemeSelect = {
                onThemeSelect(it)
                showSheet = false
            },
            onDismissRequest = { showSheet = false },
        )
    }
}

@Composable
private fun ThemeSelectionBottomSheet(
    currentTheme: String,
    onThemeSelect: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ThemeSheetContent(
            currentTheme = currentTheme,
            onThemeSelect = onThemeSelect,
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun ThemeSheetContent(
    currentTheme: String,
    onThemeSelect: (String) -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Text(
            text = AppStrings.settingsThemeLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        listOf(
            "system" to AppStrings.settingsThemeSystem,
            "light" to AppStrings.settingsThemeLight,
            "dark" to AppStrings.settingsThemeDark,
        ).forEach { (value, label) ->
            ThemeOptionRow(
                label = label,
                selected = currentTheme == value,
                onClick = {
                    onThemeSelect(value)
                    onCloseClick()
                },
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = AppDimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}
