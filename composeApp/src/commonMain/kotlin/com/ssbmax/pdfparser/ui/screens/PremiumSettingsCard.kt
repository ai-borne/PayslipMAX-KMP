package com.ssbmax.pdfparser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumSettingsCard(
    isPremiumEnabled: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
    geminiApiKey: String,
    onApiKeyChange: (String) -> Unit,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PremiumHeaderRow(
            isPremiumEnabled = isPremiumEnabled,
            onPremiumToggle = onPremiumToggle,
            onUpgradePrompt = onUpgradePrompt,
        )
        AnimatedVisibility(visible = isPremiumEnabled) {
            GeminiApiKeyInput(
                geminiApiKey = geminiApiKey,
                onApiKeyChange = onApiKeyChange,
                modifier = Modifier.padding(horizontal = AppDimensions.PaddingMedium).padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun PremiumHeaderRow(
    isPremiumEnabled: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitleText =
        if (isPremiumEnabled) {
            AppStrings.settingsProPlanActive
        } else {
            "${AppStrings.settingsProPlanDesc} (${AppStrings.settingsProPlanPrice})"
        }

    SettingsRow(
        icon = "👑",
        title = AppStrings.settingsProPlanTitle,
        subtitle = subtitleText,
        trailingContent = {
            Switch(
                checked = isPremiumEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        onUpgradePrompt()
                    } else {
                        onPremiumToggle(false)
                    }
                },
            )
        },
        onClick = if (!isPremiumEnabled) onUpgradePrompt else null,
        modifier = modifier,
    )
}

@Composable
private fun GeminiApiKeyInput(
    geminiApiKey: String,
    onApiKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiKeyVisible by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = geminiApiKey,
            onValueChange = onApiKeyChange,
            label = { Text(AppStrings.settingsApiKeyLabel) },
            placeholder = { Text(AppStrings.settingsApiKeyPlaceholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                    Text(if (apiKeyVisible) "👁️" else "🙈")
                }
            },
        )
        Text(
            text = AppStrings.settingsApiKeyFooter,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
