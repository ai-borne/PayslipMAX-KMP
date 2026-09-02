package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

/**
 * Phase 6: mirrors PCDA's own page-4 tax computation verbatim -- the TDS-track anchor the
 * reconciliation card (and ADR-3 generally) reconciles our engine's liability against. Never
 * recomputed; these are the payslip's own printed figures.
 */
@Composable
fun TaxPcdaOfficialComputationCard(
    taxAndSavings: TaxAndSavings,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsTaxPlanner.pcdaCardTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStringsTaxPlanner.pcdaCardSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        PcdaFigureRow(AppStringsTaxPlanner.pcdaGrossYtdLabel, taxAndSavings.grossSalaryYtd)
        PcdaFigureRow(AppStringsTaxPlanner.pcdaTaxableIncomeLabel, taxAndSavings.totalTaxableIncome)
        PcdaFigureRow(AppStringsTaxPlanner.pcdaStandardDeductionLabel, taxAndSavings.standardDeduction)
        PcdaFigureRow(AppStringsTaxPlanner.pcdaNetTaxableLabel, taxAndSavings.netTaxableIncome, emphasize = true)

        val totalTaxPayable = taxAndSavings.totalTaxPayable
        if (totalTaxPayable != null) {
            PcdaFigureRow(AppStringsTaxPlanner.pcdaTotalTaxPayableLabel, totalTaxPayable, emphasize = true)
        } else {
            Text(
                text = AppStringsTaxPlanner.pcdaTaxUnavailableNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        PcdaFigureRow(AppStringsTaxPlanner.pcdaTaxDeductedYtdLabel, taxAndSavings.taxDeductedYtd)
        PcdaFigureRow(AppStringsTaxPlanner.pcdaCessDeductedYtdLabel, taxAndSavings.cessDeductedYtd)
    }
}

@Composable
private fun PcdaFigureRow(
    label: String,
    amount: Double,
    emphasize: Boolean = false,
) {
    Text(
        text = "$label${TaxLedgerAggregator.formatIndianCurrency(amount)}",
        style = if (emphasize) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
        fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}
