@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumUpgradeBottomSheet(
    onDismissRequest: () -> Unit,
    onUnlockClick: () -> Unit,
    price: String = AppStrings.settingsPremiumPlanPrice,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        UpgradeSheetContent(
            price = price,
            onUnlockClick = {
                onUnlockClick()
                onDismissRequest()
            },
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun UpgradeSheetContent(
    price: String,
    onUnlockClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        UpgradeHeaderSection()
        UpgradeBenefitsSection()
        UpgradePricingSection(price = price)
        UpgradeActionsSection(onUnlockClick = onUnlockClick, onCloseClick = onCloseClick)
    }
}

@Composable
private fun UpgradeHeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text(AppStrings.premiumTeaserCrownIcon, fontSize = AppDimensions.FontSizeEmojiMedium)
        Text(
            text = AppStrings.settingsPremiumPlanTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStrings.settingsPremiumPlanDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UpgradeBenefitsSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = AppStrings.settingsPremiumPlanBullet1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBullet2,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBullet3,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBullet4,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun UpgradePricingSection(price: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
    ) {
        Text(
            text = price,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBillingNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpgradeActionsSection(
    onUnlockClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = onUnlockClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(AppStrings.settingsPremiumUpgradeBtn)
        }
        TextButton(
            onClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(AppStrings.btnCancel)
        }
    }
}
