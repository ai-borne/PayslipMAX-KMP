package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.restorePurchases

/**
 * The premium upgrade sheet wired to the purchase, restore and price flows of [viewModel]. A screen that
 * keeps the privacy policy in-app passes [onPrivacyClick]; otherwise the sheet opens the web page.
 */
@Composable
internal fun PayslipUpgradeSheet(
    viewModel: PayslipViewModel,
    onDismiss: () -> Unit,
    onPrivacyClick: (() -> Unit)? = null,
) {
    val premiumPrice by viewModel.premiumPriceState.collectAsState()
    PremiumUpgradeBottomSheet(
        onDismissRequest = onDismiss,
        onUnlockClick = { onResult -> viewModel.launchPurchaseFlow(onResult) },
        onRestoreClick = { onResult -> viewModel.restorePurchases(onResult) },
        onPrivacyClick = onPrivacyClick,
        price = premiumPrice,
        onPresented = viewModel::refreshPremiumPrice,
    )
}
