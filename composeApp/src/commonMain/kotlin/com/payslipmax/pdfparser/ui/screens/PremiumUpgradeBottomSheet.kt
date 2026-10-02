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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

/**
 * How long a success confirmation stays on screen before the sheet closes itself.
 *
 * Both success branches used to set the message and call `onDismissRequest()` in the same breath,
 * so the confirmation rendered into a sheet already being torn down — a *successful* purchase or
 * restore showed a spinner, then silence (Phase 7 debt, `docs/Launch/08_ios_monetization_phaseplan.md`).
 * That actively misleads a user, and misled Phase 7's own testing.
 */
internal const val SUCCESS_FEEDBACK_VISIBLE_MS = 1_500L

/**
 * How long the sheet stays open after [outcome] so its message can actually be read, or null to
 * stay open indefinitely (the user closes it). Pure and Compose-free, like the outcome mapping
 * above, so the rule is unit-testable without standing up a modal-sheet window.
 */
internal fun dismissDelayMsFor(outcome: PurchaseSheetOutcome): Long? =
    when (outcome) {
        is PurchaseSheetOutcome.Success -> SUCCESS_FEEDBACK_VISIBLE_MS
        is PurchaseSheetOutcome.StayOpen, is PurchaseSheetOutcome.ShowError -> null
    }

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

/** The banner a result shows, or null when the sheet should stay as it is. */
private fun PurchaseSheetOutcome.feedback(): BackupStatus? =
    when (this) {
        is PurchaseSheetOutcome.Success -> BackupStatus(message, isSuccess = true)
        is PurchaseSheetOutcome.ShowError -> BackupStatus(message, isSuccess = false)
        is PurchaseSheetOutcome.StayOpen -> null
    }

@Composable
private fun SheetLifecycleEffects(
    onPresented: () -> Unit,
    pendingDismissDelayMs: Long?,
    onDismissRequest: () -> Unit,
) {
    // Re-read the store price as the sheet opens: the startup read can predate StoreKit resolving
    // the storefront, and this sheet is where the quoted price becomes a commitment.
    LaunchedEffect(Unit) { onPresented() }

    LaunchedEffect(pendingDismissDelayMs) {
        pendingDismissDelayMs?.let { delayMs ->
            delay(delayMs)
            onDismissRequest()
        }
    }
}

@Composable
fun PremiumUpgradeBottomSheet(
    onDismissRequest: () -> Unit,
    onUnlockClick: (onResult: (PurchaseResult) -> Unit) -> Unit,
    onRestoreClick: (onResult: (PurchaseResult) -> Unit) -> Unit = {},
    onTermsClick: (() -> Unit)? = null,
    onPrivacyClick: (() -> Unit)? = null,
    price: String? = null,
    onPresented: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isPurchasing by rememberSaveable { mutableStateOf(false) }
    var isRestoring by rememberSaveable { mutableStateOf(false) }
    var feedbackStatus by remember { mutableStateOf<BackupStatus?>(null) }
    var pendingDismissDelayMs by remember { mutableStateOf<Long?>(null) }

    SheetLifecycleEffects(onPresented, pendingDismissDelayMs, onDismissRequest)

    // One purchase-or-restore flow at a time; the outcome decides the banner and whether the sheet closes itself.
    fun runFlow(
        setBusy: (Boolean) -> Unit,
        start: (onResult: (PurchaseResult) -> Unit) -> Unit,
        toOutcome: (PurchaseResult) -> PurchaseSheetOutcome,
    ) {
        if (isPurchasing || isRestoring) return
        setBusy(true)
        feedbackStatus = null
        start { result ->
            setBusy(false)
            val outcome = toOutcome(result)
            feedbackStatus = outcome.feedback() ?: feedbackStatus
            pendingDismissDelayMs = dismissDelayMsFor(outcome)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        UpgradeSheetContent(
            price = price, isPurchasing = isPurchasing, isRestoring = isRestoring, feedbackStatus = feedbackStatus,
            onUnlockClick = { runFlow({ isPurchasing = it }, onUnlockClick, ::purchaseSheetOutcome) },
            onRestoreClick = { runFlow({ isRestoring = it }, onRestoreClick, ::restoreSheetOutcome) },
            onCloseClick = onDismissRequest,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
        )
    }
}

@Composable
private fun UpgradeSheetContent(
    price: String?,
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
            canPurchase = price != null,
            onUnlockClick = onUnlockClick,
            onRestoreClick = onRestoreClick,
            onCloseClick = onCloseClick,
        )
        UpgradeLegalFooter(onTermsClick = onTermsClick, onPrivacyClick = onPrivacyClick)
        feedbackStatus?.let { StatusMessage(status = it) }
    }
}
