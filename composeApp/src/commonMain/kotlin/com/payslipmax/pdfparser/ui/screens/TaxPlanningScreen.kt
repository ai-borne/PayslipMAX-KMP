package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.insights.WealthOptimizationEngine
import com.payslipmax.pdfparser.tax.TaxRuleKnowledgeBase
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun TaxPlanningScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState.collectAsState().value
    val optimizationResult =
        remember(uiState.payslips, uiState.selectedPayslip) {
            if (uiState.payslips.isNotEmpty()) {
                WealthOptimizationEngine.analyzeLedger(uiState.payslips, uiState.selectedPayslip)
            } else {
                null
            }
        }
    TaxPlanningScreen(
        optimizationResult = optimizationResult,
        onNavigateBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxPlanningScreen(
    optimizationResult: OptimizationResult?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStringsPremium.taxPlanningTitle,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = AppStringsPremium.taxPlanningSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { innerPadding ->
        if (optimizationResult == null) {
            TaxPlanningEmptyState()
        } else {
            TaxPlanningContent(
                optimizationResult = optimizationResult,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun TaxPlanningContent(
    optimizationResult: OptimizationResult,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        optimizationResult.storyNarrative?.let { narrative ->
            TaxNarrativeBenchmarkCard(narrative = narrative)
            TaxNarrativeLedgerCard(narrative = narrative)
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

        TaxRuleVersionFooter(financialYear = optimizationResult.fySummary?.financialYear ?: "2026-27")
    }
}

@Composable
private fun TaxRuleVersionFooter(
    financialYear: String,
) {
    val rules = TaxRuleKnowledgeBase.getRulesForFy(financialYear)
    val footerText = "🛡️ Tax Engine: CBDT Rules FY ${rules.financialYear} (AY ${rules.assessmentYear}) · Version 2026.1 (Verified ${rules.lastVerifiedDate}) · 100% Offline Secured"

    Text(
        text = footerText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.PaddingSmall),
    )
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
