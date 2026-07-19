package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.clearAiInsights
import com.payslipmax.pdfparser.ui.hasAccess

/**
 * InsightsLazyBody's item groups, split out so InsightsScreen.kt stays under the 300-line file
 * limit — pure [LazyListScope] wiring, no new logic beyond what each component already exposes.
 *
 * This group ([MonthlySnapshot] + [SmartInsightsSection] + [PayTrendChart]) is also kept out of
 * InsightsLazyBody itself so that composable stays under the 50-line limit; a plain [LazyListScope]
 * extension has no such cap.
 */
fun LazyListScope.insightsPrimaryItems(
    state: InsightsState,
    smartInsights: List<InsightUiModel>,
    wellnessExpanded: Boolean,
    onWellnessExpandClick: () -> Unit,
    hasWealthOptimization: Boolean,
    hasAccess: (FeatureGate) -> Boolean,
    onShowUpgradeSheet: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    item {
        MonthlySnapshot(
            state = state,
            wellnessExpanded = wellnessExpanded,
            onWellnessExpandClick = onWellnessExpandClick,
            // Wealth-optimization figures live only in this chip now (D4 gate) — see Phase 4 deep-check.
            onSeeHowClick = { if (hasWealthOptimization) onNavigateTo(Screen.TaxPlanning) else onShowUpgradeSheet() },
        )
    }
    item {
        SmartInsightsSection(
            insights = smartInsights,
            hasAccess = hasAccess,
            onActionClick = onNavigateTo,
            onUpgradeClick = onShowUpgradeSheet,
        )
    }
    item { PayTrendChart(history = state.historySorted, selected = state.currentRecord) }
}

/** [RecommendedActions] + [AdvancedAnomaliesCard] + the Premium Report — same 50-line-cap rationale
 *  as [insightsPrimaryItems]. */
fun LazyListScope.insightsActionItems(
    state: InsightsState,
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
    smartInsights: List<InsightUiModel>,
    hasAnomalyDetection: Boolean,
    hasPremiumIntelligence: Boolean,
    toolsExpanded: Boolean,
    onToolsExpandClick: () -> Unit,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    item {
        RecommendedActions(
            state = state,
            shownInsights = smartInsights,
            hasAccess = { gate -> viewModel.hasAccess(gate) },
            onNavigateTo = onNavigateTo,
            onUpgradeClick = onShowUpgradeSheet,
        )
    }
    item { AdvancedAnomaliesCard(state.engineResult.anomalies, hasAnomalyDetection, onShowUpgradeSheet) }
    item {
        InsightsPremiumReportItem(
            uiState = uiState,
            isPremiumEnabled = hasPremiumIntelligence,
            toolsExpanded = toolsExpanded,
            onToolsExpandClick = onToolsExpandClick,
            viewModel = viewModel,
            onShowUpgradeSheet = onShowUpgradeSheet,
            onShowTransparency = onShowTransparency,
            onViewInsightsClick = onViewInsightsClick,
            onNavigateTo = onNavigateTo,
        )
    }
}

@Composable
private fun InsightsPremiumReportItem(
    uiState: PayslipUiState,
    isPremiumEnabled: Boolean,
    toolsExpanded: Boolean,
    onToolsExpandClick: () -> Unit,
    viewModel: PayslipViewModel,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    PremiumReportCard(
        isPremiumEnabled = isPremiumEnabled,
        toolsExpanded = toolsExpanded,
        onToolsExpandClick = onToolsExpandClick,
        onUpgradeClick = onShowUpgradeSheet,
        onNavigateTo = onNavigateTo,
        aiSectionContent = {
            GeminiAiInsightsSection(
                aiInsights = uiState.aiInsights,
                isAiLoading = uiState.isAiLoading,
                aiError = uiState.aiError,
                onGenerateClick = onShowTransparency,
                onViewInsightsClick = onViewInsightsClick,
                onClearClick = { viewModel.clearAiInsights() },
            )
        },
    )
}
