package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumSettingsCard(
    isPremiumEnabled: Boolean,
    onUpgradePrompt: () -> Unit,
    price: String = AppStrings.settingsPremiumPlanPrice,
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = getPremiumCardContainerColor(isPremiumEnabled, isDark)
    val borderColor = getPremiumCardBorderColor(isPremiumEnabled)

    Card(
        modifier =
            modifier
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
        PremiumCardContent(isPremiumEnabled = isPremiumEnabled, price = price)
    }
}

@Composable
fun PremiumSettingsCardContentRow(
    isPremiumEnabled: Boolean,
    onUpgradePrompt: () -> Unit,
    price: String = AppStrings.settingsPremiumPlanPrice,
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = getPremiumCardContainerColor(isPremiumEnabled, isDark)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    if (!isPremiumEnabled) {
                        onUpgradePrompt()
                    }
                },
        color = containerColor,
    ) {
        PremiumCardContent(isPremiumEnabled = isPremiumEnabled, price = price)
    }
}

@Composable
private fun getPremiumCardContainerColor(
    isPremiumEnabled: Boolean,
    isDark: Boolean,
) =
    if (isPremiumEnabled) {
        if (isDark) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        }
    } else {
        if (isDark) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
        }
    }

@Composable
private fun getPremiumCardBorderColor(isPremiumEnabled: Boolean) =
    if (isPremiumEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    }

@Composable
private fun PremiumCardContent(
    isPremiumEnabled: Boolean,
    price: String,
) {
    Row(
        modifier =
            Modifier
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
                text = AppStrings.premiumTeaserCrownIcon,
                fontSize = AppDimensions.TextSizeHuge,
                modifier = Modifier.padding(end = AppDimensions.SpacingMedium),
            )
            PremiumTextDetails(isPremiumEnabled = isPremiumEnabled, price = price)
        }
        if (!isPremiumEnabled) {
            Text(
                text = "➔",
                fontSize = AppDimensions.TextSizeLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun PremiumTextDetails(
    isPremiumEnabled: Boolean,
    price: String,
) {
    Column {
        Text(
            text = if (isPremiumEnabled) AppStrings.settingsPremiumPlanActive else AppStrings.settingsPremiumPlanUpgradeTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPremiumEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text =
                if (isPremiumEnabled) {
                    AppStrings.settingsPremiumPlanSubscribedNote
                } else {
                    "${AppStrings.settingsPremiumPlanUpgradeSubtitlePrefix} ($price)"
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
