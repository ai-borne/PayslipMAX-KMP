package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.screens.PayslipUpgradeSheet
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.part2FactoryOrNull
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.preClaimFactoryOrNull
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/**
 * The Guide tab as `App` hosts it: [GuideTab] wired to the app's entitlement and to the existing upgrade sheet, so the
 * Guide itself never touches billing. The sheet is the same one Pay Audit and the other Premium surfaces open.
 */
@Composable
fun GuideTabRoute(
    navState: GuideNavState,
    viewModel: PayslipViewModel,
) {
    var showUpgradeSheet by remember { mutableStateOf(false) }
    val unlocked = viewModel.rememberGuideUnlocked()
    val koin = getKoin()
    GuideTab(
        navState = navState,
        access = GuideAccess(unlocked, onUnlock = { showUpgradeSheet = true }),
        notesViewModel = koinInject(),
        // Off by default (LaunchFlags.PRECLAIM_ENABLED): the factory is null, so no pre-claim entry exists and Koin is never asked for the worksheet.
        preClaim = preClaimFactoryOrNull { koin.get<PreClaimViewModel>() },
        // Off by default (LaunchFlags.PART2_ENABLED): the factory is null, so no Learn entry exists and Koin is never asked for the model.
        part2 = part2FactoryOrNull { koin.get<Part2LookupViewModel>() },
        part2Bank = part2FactoryOrNull { koin.get<BankViewModel>() },
        bankAccess = BankAccess(viewModel.rememberHasAccess(FeatureGate.PART2_ORDERS), onUnlock = { showUpgradeSheet = true }),
    )
    if (showUpgradeSheet) PayslipUpgradeSheet(viewModel, onDismiss = { showUpgradeSheet = false })
}
