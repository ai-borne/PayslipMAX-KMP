package com.ssbmax.pdfparser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumSettingsCard(
    isPremiumEnabled: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
    geminiApiKey: String,
    onApiKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumHeaderRow(
                isPremiumEnabled = isPremiumEnabled,
                onPremiumToggle = onPremiumToggle,
            )
            AnimatedVisibility(visible = isPremiumEnabled) {
                GeminiApiKeyInput(
                    geminiApiKey = geminiApiKey,
                    onApiKeyChange = onApiKeyChange,
                )
            }
        }
    }
}

@Composable
private fun PremiumHeaderRow(
    isPremiumEnabled: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👑", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
            Column {
                Text(
                    text = AppStrings.settingsProPlanTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        if (isPremiumEnabled) {
                            AppStrings.settingsProPlanActive
                        } else {
                            "${AppStrings.settingsProPlanDesc} (${AppStrings.settingsProPlanPrice})"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isPremiumEnabled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        Switch(checked = isPremiumEnabled, onCheckedChange = onPremiumToggle)
    }
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
            visualTransformation =
                if (apiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
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

