package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/** Renders [buildSmartInsights] as an ordered stack of [InsightCard]s — the redesign's primary signal surface. */
@Composable
fun SmartInsightsSection(
    state: InsightsState,
    onActionClick: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val insights = remember(state) { buildSmartInsights(state) }
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
        insights.forEach { insight -> InsightCard(insight = insight, onActionClick = onActionClick) }
    }
}
