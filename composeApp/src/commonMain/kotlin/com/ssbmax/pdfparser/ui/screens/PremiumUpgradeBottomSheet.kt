@file:OptIn(ExperimentalMaterial3Api::class)

package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumUpgradeBottomSheet(
    onDismissRequest: () -> Unit,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        UpgradeSheetContent(
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
        UpgradePricingSection()
        UpgradeActionsSection(onUnlockClick = onUnlockClick, onCloseClick = onCloseClick)
    }
}

@Composable
private fun UpgradeHeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text("👑", fontSize = AppDimensions.FontSizeEmojiMedium)
        Text(
            text = AppStrings.settingsProPlanTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStrings.settingsProPlanDesc,
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
            text = AppStrings.settingsProPlanBullet1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsProPlanBullet2,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsProPlanBullet3,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsProPlanBullet4,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun UpgradePricingSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
    ) {
        Text(
            text = AppStrings.settingsProPlanPrice,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStrings.settingsProPlanBillingNote,
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
            Text(AppStrings.settingsProUpgradeBtn)
        }
        TextButton(
            onClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(AppStrings.btnCancel)
        }
    }
}
