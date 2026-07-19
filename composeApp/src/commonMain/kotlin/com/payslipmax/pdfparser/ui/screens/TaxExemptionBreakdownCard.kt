package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.TaxExemptionBreakdown
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun TaxExemptionBreakdownCard(
    exemptionBreakdown: TaxExemptionBreakdown,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
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
            text = "₹$amountText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
