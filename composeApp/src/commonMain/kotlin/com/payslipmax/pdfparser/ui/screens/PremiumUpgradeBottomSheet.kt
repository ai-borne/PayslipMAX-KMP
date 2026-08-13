@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.billing.PurchaseResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/** What the upgrade sheet should do once a [PurchaseResult] comes back — pure, unit-testable without Compose. */
internal sealed interface PurchaseSheetOutcome {
    data object Dismiss : PurchaseSheetOutcome

    data object StayOpen : PurchaseSheetOutcome

    data class ShowError(val message: String) : PurchaseSheetOutcome
}

internal fun purchaseSheetOutcome(result: PurchaseResult): PurchaseSheetOutcome =
    when (result) {
        is PurchaseResult.Success -> PurchaseSheetOutcome.Dismiss
        is PurchaseResult.UserCancelled, is PurchaseResult.Pending -> PurchaseSheetOutcome.StayOpen
        is PurchaseResult.Error -> PurchaseSheetOutcome.ShowError("${AppStrings.statusPurchaseFailed}${result.message}")
    }

@Composable
fun PremiumUpgradeBottomSheet(
    onDismissRequest: () -> Unit,
    onUnlockClick: (onResult: (PurchaseResult) -> Unit) -> Unit,
    price: String = AppStrings.settingsPremiumPlanPrice,
    modifier: Modifier = Modifier,
) {
    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        UpgradeSheetContent(
            price = price,
            isPurchasing = isPurchasing,
            purchaseError = purchaseError,
            onUnlockClick = {
                isPurchasing = true
                purchaseError = null
                onUnlockClick { result ->
                    isPurchasing = false
                    when (val outcome = purchaseSheetOutcome(result)) {
                        is PurchaseSheetOutcome.Dismiss -> onDismissRequest()
                        is PurchaseSheetOutcome.StayOpen -> Unit
                        is PurchaseSheetOutcome.ShowError -> purchaseError = outcome.message
                    }
                }
            },
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun UpgradeSheetContent(
    price: String,
    isPurchasing: Boolean,
    purchaseError: String?,
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
        UpgradeActionsSection(
            isPurchasing = isPurchasing,
            onUnlockClick = onUnlockClick,
            onCloseClick = onCloseClick,
        )
        purchaseError?.let { StatusMessage(status = BackupStatus(it, isSuccess = false)) }
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
    isPurchasing: Boolean,
    onUnlockClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = onUnlockClick,
            enabled = !isPurchasing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimensions.IconSizeMedium),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(AppStrings.settingsPremiumUpgradeBtn)
            }
        }
        TextButton(
            onClick = onCloseClick,
            enabled = !isPurchasing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(AppStrings.btnCancel)
        }
    }
}
