package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.FyTaxLedgerSummary
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

@Composable
fun TaxFyRunwayHeaderCard(
    fySummary: FyTaxLedgerSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${AppStringsPremium.taxPlanningFyStatusPrefix}${fySummary.financialYear} (AY ${fySummary.assessmentYear})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                val monthSuffix = if (fySummary.parsedMonthCount == 1) AppStringsTaxPlanner.fyMonthSingular else AppStringsTaxPlanner.fyMonthPlural
                Text(
                    text = "${AppStringsTaxPlanner.fyMonthsParsedPrefix}${fySummary.parsedMonthCount}$monthSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Surface(
                shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Text(
                    text = "${AppStringsPremium.taxPlanningPreliminaryEstimatePrefix}${fySummary.parsedMonthCount}${AppStringsPremium.taxPlanningOfMonthsSuffix}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTiny),
                )
            }
        }
    }
}
