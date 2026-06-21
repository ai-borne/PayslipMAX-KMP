package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumSettingsCard(
    isPremiumEnabled: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumHeaderRow(
        isPremiumEnabled = isPremiumEnabled,
        onPremiumToggle = onPremiumToggle,
        onUpgradePrompt = onUpgradePrompt,
        modifier = modifier,
    )
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
