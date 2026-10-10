package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.PayslipUpgradeSheet
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
    GuideTab(navState = navState, access = GuideAccess(unlocked, onUnlock = { showUpgradeSheet = true }), notesViewModel = koinInject())
    if (showUpgradeSheet) PayslipUpgradeSheet(viewModel, onDismiss = { showUpgradeSheet = false })
}
