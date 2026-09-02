package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.FyTaxLedgerSummary
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

/**
 * One card, stacked vertically top-to-bottom (which month is active -> which FY -> coverage ->
 * coverage badge) instead of a title/badge Row squeezed side-by-side -- the Row layout truncated
 * both the FY title and the "months parsed" line on real device widths once a badge shared the row.
 * [activePayslipLabel] (Phase 8, U2) is folded in here rather than rendered as its own separate line
 * above the card, so "which month is this" and "which FY is this" read as one coherent status block.
 */
@Composable
fun TaxFyRunwayHeaderCard(
    fySummary: FyTaxLedgerSummary,
    activePayslipLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            activePayslipLabel?.let { TaxDataAsOfBanner(activePayslipLabel = it) }
            Text(
                text = "${AppStringsPremium.taxPlanningFyStatusPrefix}${fySummary.financialYear} (AY ${fySummary.assessmentYear})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val monthSuffix = if (fySummary.parsedMonthCount == 1) AppStringsTaxPlanner.fyMonthSingular else AppStringsTaxPlanner.fyMonthPlural
            Text(
                text = "${AppStringsTaxPlanner.fyMonthsParsedPrefix}${fySummary.parsedMonthCount}$monthSuffix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
            PreliminaryEstimateBadge(parsedMonthCount = fySummary.parsedMonthCount)
        }
    }
}

@Composable
private fun PreliminaryEstimateBadge(parsedMonthCount: Int) {
    Surface(
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = "${AppStringsPremium.taxPlanningPreliminaryEstimatePrefix}$parsedMonthCount${AppStringsPremium.taxPlanningOfMonthsSuffix}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTiny),
        )
    }
}
