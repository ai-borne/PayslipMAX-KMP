package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.insights.TaxExemptionBreakdown
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

@Composable
fun TaxExemptionBreakdownCard(
    exemptionBreakdown: TaxExemptionBreakdown,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsPremium.taxPlanningExemptionsCardTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        ExemptionRowItem(
            label = AppStringsPremium.taxPlanning80CLabel,
            amount = exemptionBreakdown.sec80C.eligibleDeduction,
        )
        ExemptionRowItem(
            label = AppStringsPremium.taxPlanningFieldLabel,
            amount = exemptionBreakdown.fieldAllowanceExemption,
        )
        ExemptionRowItem(
            label = AppStringsPremium.taxPlanningHraLabel,
            amount = exemptionBreakdown.hraExemption,
        )
        ExemptionRowItem(
            label = AppStringsPremium.taxPlanning80CCD1BLabel,
            amount = exemptionBreakdown.sec80CCD1B.eligibleDeduction,
        )

        ExemptionDisclosureNote(exemptionBreakdown = exemptionBreakdown)

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        val totalText = TaxLedgerAggregator.formatIndianCurrency(exemptionBreakdown.totalOldRegimeDeductions)
        Text(
            text = "${AppStringsPremium.taxPlanningExemptionsTotalLabel}$totalText",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** D10/Open Item 2: the two "not a silent zero/assumption" disclosures, never both at once. */
@Composable
private fun ExemptionDisclosureNote(
    exemptionBreakdown: TaxExemptionBreakdown,
) {
    if (exemptionBreakdown.regime == TaxRegime.NEW) {
        Text(
            text = AppStringsPremium.taxPlanningExemptionsUnavailableNewRegimeNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    } else if (exemptionBreakdown.fieldCapIsConservativeAssumption) {
        Text(
            text = AppStringsPremium.taxPlanningFieldCapConservativeAssumptionNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExemptionRowItem(
    label: String,
    amount: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val amountText = TaxLedgerAggregator.formatIndianCurrency(amount)
        Text(
            text = "${AppStringsTaxPlanner.rupeeSymbol}$amountText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
