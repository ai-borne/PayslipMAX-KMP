package com.ssbmax.pdfparser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun SettingsHelpDocs(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsCategoryHeader(title = AppStrings.settingsDocumentationHeader)
        SettingsCategoryCard {
            HelpDocItem(title = AppStrings.settingsHelpFaqTitle) { FaqContent() }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            HelpDocItem(title = AppStrings.settingsHelpPrivacyTitle) { PrivacyContent() }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            HelpDocItem(title = AppStrings.settingsHelpAiTitle) { AiPolicyContent() }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            HelpDocItem(title = AppStrings.settingsHelpDisclaimerTitle) { DisclaimerContent() }
        }
    }
}

@Composable
private fun HelpDocItem(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = AppDimensions.PaddingMedium, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = AppDimensions.PaddingMedium)
                        .padding(bottom = AppDimensions.PaddingMedium),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun FaqContent() {
    Text(
        text = AppStrings.settingsHelpFaqContent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PrivacyContent() {
    Text(
        text = AppStrings.settingsHelpPrivacyContent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AiPolicyContent() {
    Text(
        text = AppStrings.settingsHelpAiContent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DisclaimerContent() {
    Text(
        text = AppStrings.settingsHelpDisclaimerContent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
