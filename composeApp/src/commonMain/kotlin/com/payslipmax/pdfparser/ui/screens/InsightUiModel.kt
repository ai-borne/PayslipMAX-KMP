package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.insights.InsightSeverity

/**
 * Presentation model for a single Smart Insights card. Built by [buildSmartInsights] from
 * [InsightsState] — never constructed ad hoc inside a composable, so severity/copy stays consistent
 * wherever a card is rendered.
 */
data class InsightUiModel(
    val title: String,
    val explanation: String,
    val severity: InsightSeverity,
    val amountLabel: String? = null,
    val actionLabel: String? = null,
    val actionTarget: Screen? = null,
)
