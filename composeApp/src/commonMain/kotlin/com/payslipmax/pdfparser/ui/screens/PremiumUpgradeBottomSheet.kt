@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.billing.PurchaseResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/** What the upgrade sheet should do once a [PurchaseResult] comes back — pure, unit-testable without Compose. */
internal sealed interface PurchaseSheetOutcome {
    data class Success(val message: String) : PurchaseSheetOutcome

    data object StayOpen : PurchaseSheetOutcome

    data class ShowError(val message: String) : PurchaseSheetOutcome
}

internal fun purchaseSheetOutcome(result: PurchaseResult): PurchaseSheetOutcome =
    when (result) {
        is PurchaseResult.Success -> PurchaseSheetOutcome.Success(AppStrings.statusPurchaseSuccess)
        is PurchaseResult.UserCancelled, is PurchaseResult.Pending -> PurchaseSheetOutcome.StayOpen
        is PurchaseResult.Error -> PurchaseSheetOutcome.ShowError("${AppStrings.statusPurchaseFailed}${result.message}")
    }

internal fun restoreSheetOutcome(result: PurchaseResult): PurchaseSheetOutcome =
    when (result) {
        is PurchaseResult.Success -> PurchaseSheetOutcome.Success(AppStrings.statusRestorePurchasesSuccess)
        is PurchaseResult.UserCancelled, is PurchaseResult.Pending -> PurchaseSheetOutcome.StayOpen
        is PurchaseResult.Error -> PurchaseSheetOutcome.ShowError("${AppStrings.statusRestorePurchasesFailed}${result.message}")
    }

@Composable
fun PremiumUpgradeBottomSheet(
    onDismissRequest: () -> Unit,
    onUnlockClick: (onResult: (PurchaseResult) -> Unit) -> Unit,
    onRestoreClick: (onResult: (PurchaseResult) -> Unit) -> Unit = {},
    onTermsClick: (() -> Unit)? = null,
    onPrivacyClick: (() -> Unit)? = null,
    price: String = AppStrings.settingsPremiumPlanPrice,
    modifier: Modifier = Modifier,
) {
    var isPurchasing by rememberSaveable { mutableStateOf(false) }
    var isRestoring by rememberSaveable { mutableStateOf(false) }
    var feedbackStatus by remember { mutableStateOf<BackupStatus?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        UpgradeSheetContent(
            price = price,
            isPurchasing = isPurchasing,
            isRestoring = isRestoring,
            feedbackStatus = feedbackStatus,
            onUnlockClick = {
                if (isPurchasing || isRestoring) return@UpgradeSheetContent
                isPurchasing = true
                feedbackStatus = null
                onUnlockClick { result ->
                    isPurchasing = false
                    when (val outcome = purchaseSheetOutcome(result)) {
                        is PurchaseSheetOutcome.Success -> {
                            feedbackStatus = BackupStatus(outcome.message, isSuccess = true)
                            onDismissRequest()
                        }
                        is PurchaseSheetOutcome.StayOpen -> Unit
                        is PurchaseSheetOutcome.ShowError -> {
                            feedbackStatus = BackupStatus(outcome.message, isSuccess = false)
                        }
                    }
                }
            },
            onRestoreClick = {
                if (isPurchasing || isRestoring) return@UpgradeSheetContent
                isRestoring = true
                feedbackStatus = null
                onRestoreClick { result ->
                    isRestoring = false
                    when (val outcome = restoreSheetOutcome(result)) {
                        is PurchaseSheetOutcome.Success -> {
                            feedbackStatus = BackupStatus(outcome.message, isSuccess = true)
                            onDismissRequest()
                        }
                        is PurchaseSheetOutcome.StayOpen -> Unit
                        is PurchaseSheetOutcome.ShowError -> {
                            feedbackStatus = BackupStatus(outcome.message, isSuccess = false)
                        }
                    }
                }
            },
            onCloseClick = onDismissRequest,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
        )
    }
}

@Composable
private fun UpgradeSheetContent(
    price: String,
    isPurchasing: Boolean,
    isRestoring: Boolean,
    feedbackStatus: BackupStatus?,
    onUnlockClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onCloseClick: () -> Unit,
    onTermsClick: (() -> Unit)?,
    onPrivacyClick: (() -> Unit)?,
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
            isRestoring = isRestoring,
            onUnlockClick = onUnlockClick,
            onRestoreClick = onRestoreClick,
            onCloseClick = onCloseClick,
        )
        UpgradeLegalFooter(onTermsClick = onTermsClick, onPrivacyClick = onPrivacyClick)
        feedbackStatus?.let { StatusMessage(status = it) }
    }
}
