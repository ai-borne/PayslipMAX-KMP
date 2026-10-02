package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.restorePurchases

/** The premium upgrade sheet wired to the purchase, restore and price flows of [viewModel]. */
@Composable
internal fun PayslipUpgradeSheet(
    viewModel: PayslipViewModel,
    onDismiss: () -> Unit,
) {
    val premiumPrice by viewModel.premiumPriceState.collectAsState()
    PremiumUpgradeBottomSheet(
        onDismissRequest = onDismiss,
        onUnlockClick = { onResult -> viewModel.launchPurchaseFlow(onResult) },
        onRestoreClick = { onResult -> viewModel.restorePurchases(onResult) },
        price = premiumPrice,
        onPresented = viewModel::refreshPremiumPrice,
    )
}
