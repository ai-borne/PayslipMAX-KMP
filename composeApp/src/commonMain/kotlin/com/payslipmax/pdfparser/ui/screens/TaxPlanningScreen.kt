package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.insights.WealthOptimizationEngine
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun TaxPlanningScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    val payslipList = uiState.payslips

    val result =
        remember(selected, payslipList) {
            selected?.let { WealthOptimizationEngine.analyzeLedger(payslipList, selected) }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        ScreenBackHeader(
            title = AppStringsPremium.taxPlanningTitle,
            subtitle = AppStringsPremium.taxPlanningSubtitle,
            onBack = onBack,
        )

        if (result != null) {
            TaxPlanningContent(optimizationResult = result)
        } else {
            TaxPlanningEmptyState()
        }
    }
}

@Composable
private fun TaxPlanningContent(
    optimizationResult: OptimizationResult,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        optimizationResult.fySummary?.let { fy ->
            TaxFyRunwayHeaderCard(fySummary = fy)
        }

        optimizationResult.regimeComparison?.let { comp ->
            TaxRegimeBattleHeroCard(comparison = comp)
        }

        optimizationResult.tdsRunway?.let { tds ->
            TaxTdsRunwayProgressCard(tdsRunway = tds)
        }

        optimizationResult.exemptionBreakdown?.let { exemptions ->
            TaxExemptionBreakdownCard(exemptionBreakdown = exemptions)
        }

        TaxActionableChecklistCard(opportunities = optimizationResult.opportunities)

        TaxEducativeTipsCard()
    }
}

@Composable
private fun TaxPlanningEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppStringsPremium.taxPlanningNoProjections,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
