package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.ImportUiState
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueCardModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueImportCard
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import org.koin.compose.koinInject

/**
 * The Part II "something is due" card at the top of the Dashboard, shown after a payslip import (DECISIONS Round 25). Nothing is composed, and
 * Koin is never asked for the model, while Part II is off. [onOpenDue] takes the officer to the Due list in the Guide.
 */
@Composable
fun DashboardDueCard(
    viewModel: PayslipViewModel,
    onOpenDue: () -> Unit,
) {
    if (!isPart2Enabled()) return
    val uiState by viewModel.uiState.collectAsState()
    DueImportCard(
        model = koinInject<DueCardModel>(),
        entitled = viewModel.rememberHasAccess(FeatureGate.PART2_ORDERS),
        importDone = uiState.importUiState is ImportUiState.Success,
        onOpen = onOpenDue,
        modifier = Modifier.padding(bottom = AppDimensions.SpacingMedium),
    )
}
