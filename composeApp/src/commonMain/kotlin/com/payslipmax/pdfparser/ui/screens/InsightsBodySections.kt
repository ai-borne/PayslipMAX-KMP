package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate

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
