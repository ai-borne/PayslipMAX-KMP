package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.DsopWasteInsight
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

/**
 * Phase 5 domain / Phase 6 UI: the exact opposite of the old (uncorrected) §80CCD(1B) advice --
 * only ever shown under the New Regime with non-zero DSOP, where the contribution genuinely earns
 * no deduction. [DsopWasteInsight] is only non-null in that case (see [TwoTrackReconciliationEngine.dsopWasteInsight]).
 */
@Composable
fun TaxDsopWasteInsightCard(
    insight: DsopWasteInsight,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, tint = CardTint.Accent, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsTaxPlanner.dsopWasteCardTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = insight.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(insight.taxBenefitForgoneAnnual)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
