package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            // The badge is measured at its natural (single-line) width; the title/subtitle column
            // takes only what's left and ellipsizes instead of squeezing the badge into near-zero
            // width, which used to wrap it one character per line on longer FY labels.
            FyStatusColumn(fySummary = fySummary, modifier = Modifier.weight(1f).padding(end = AppDimensions.SpacingSmall))
            PreliminaryEstimateBadge(parsedMonthCount = fySummary.parsedMonthCount)
        }
    }
}

@Composable
private fun FyStatusColumn(
    fySummary: FyTaxLedgerSummary,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "${AppStringsPremium.taxPlanningFyStatusPrefix}${fySummary.financialYear} (AY ${fySummary.assessmentYear})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        val monthSuffix = if (fySummary.parsedMonthCount == 1) AppStringsTaxPlanner.fyMonthSingular else AppStringsTaxPlanner.fyMonthPlural
        Text(
            text = "${AppStringsTaxPlanner.fyMonthsParsedPrefix}${fySummary.parsedMonthCount}$monthSuffix",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTiny),
        )
    }
}
