package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumSettingsCard(
    isPremiumEnabled: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = getPremiumCardContainerColor(isPremiumEnabled, isDark)
    val borderColor = getPremiumCardBorderColor(isPremiumEnabled)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (!isPremiumEnabled) {
                    onUpgradePrompt()
                }
            },
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(AppDimensions.BorderThin, borderColor),
    ) {
        PremiumCardContent(isPremiumEnabled = isPremiumEnabled)
    }
}

@Composable
private fun getPremiumCardContainerColor(isPremiumEnabled: Boolean, isDark: Boolean) =
    if (isPremiumEnabled) {
        if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    } else {
        if (isDark) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
    }

@Composable
private fun getPremiumCardBorderColor(isPremiumEnabled: Boolean) =
    if (isPremiumEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    }

@Composable
private fun PremiumCardContent(isPremiumEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = AppDimensions.SpacingSmall),
        ) {
            Text(
                text = "👑",
                fontSize = AppDimensions.TextSizeHuge,
                modifier = Modifier.padding(end = AppDimensions.SpacingMedium)
            )
            PremiumTextDetails(isPremiumEnabled = isPremiumEnabled)
        }
        if (!isPremiumEnabled) {
            Text(
                text = "➔",
                fontSize = AppDimensions.TextSizeLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun PremiumTextDetails(isPremiumEnabled: Boolean) {
    Column {
        Text(
            text = if (isPremiumEnabled) AppStrings.settingsProPlanActive else "Upgrade to PayslipMax Pro",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPremiumEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = if (isPremiumEnabled) {
                "Subscribed (Next billing: Oct 2026)"
            } else {
                "Unlock Advanced Insights & Cloud Backup (₹99 / Year)"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
