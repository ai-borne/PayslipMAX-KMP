package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.subscription.FeatureGate

/**
 * Presentation model for a single Smart Insights card. Built by [buildSmartInsights] from
 * [InsightsState] — never constructed ad hoc inside a composable, so severity/copy stays consistent
 * wherever a card is rendered.
 *
 * [gate] is the [FeatureGate] protecting [actionTarget] (via [gateForScreen]), or `null` when the card
 * has no target or the target is free. [InsightCard] must check it before navigating — a card with a
 * non-null [actionTarget] and no gate check at the click site is how a PRO screen leaks to free users.
 */
data class InsightUiModel(
    val title: String,
    val explanation: String,
    val severity: InsightSeverity,
    val amountLabel: String? = null,
    val actionLabel: String? = null,
    val actionTarget: Screen? = null,
    val gate: FeatureGate? = null,
)
