package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * Renders [buildSmartInsights] output (precomputed by the caller, once, so [RecommendedActions] can
 * dedupe against the same list) as an ordered stack of [InsightCard]s — the redesign's primary signal
 * surface. [hasAccess]/[onUpgradeClick] pass straight through to each card so a gated card's action
 * button never reaches [onActionClick] for a user who lacks that entitlement.
 */
@Composable
fun SmartInsightsSection(
    insights: List<InsightUiModel>,
    hasAccess: (FeatureGate) -> Boolean,
    onActionClick: (Screen) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (insights.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Text(
            text = InsightsStrings.smartInsightsSectionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        insights.forEach { insight ->
            InsightCard(insight = insight, hasAccess = hasAccess, onActionClick = onActionClick, onUpgradeClick = onUpgradeClick)
        }
    }
}
