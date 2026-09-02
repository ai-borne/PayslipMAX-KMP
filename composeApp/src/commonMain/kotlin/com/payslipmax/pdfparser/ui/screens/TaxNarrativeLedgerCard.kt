package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.MonthlyLedgerItem
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.insights.TaxStoryNarrative
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

@Composable
fun TaxNarrativeLedgerCard(
    narrative: TaxStoryNarrative,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = narrative.coverageHeader,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        narrative.missingMonthNudge?.let { nudge ->
            MissingMonthNudgeBanner(nudgeText = nudge)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        Text(
            text = AppStringsPremium.taxPlanningNarrativeLedgerTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )

        MonthlyLedgerTable(items = narrative.monthlyLedgerList)

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        LedgerTotalsFooter(totalTds = narrative.totalTdsYtd, totalDsop = narrative.totalDsopYtd)
    }
}

@Composable
private fun MissingMonthNudgeBanner(nudgeText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.tertiary),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = "${AppStringsTaxPlanner.nudgeBannerPrefix}$nudgeText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun MonthlyLedgerTable(items: List<MonthlyLedgerItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = AppStringsPremium.taxPlanningNarrativeMonthCol, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = AppStringsPremium.taxPlanningNarrativeTdsCol, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = AppStringsPremium.taxPlanningNarrativeDsopCol, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = item.monthName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(item.tdsDeducted)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(item.dsopContribution)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LedgerTotalsFooter(
    totalTds: Double,
    totalDsop: Double,
) {
    val formattedTds = TaxLedgerAggregator.formatIndianCurrency(totalTds)
    val formattedDsop = TaxLedgerAggregator.formatIndianCurrency(totalDsop)
    val tdsPrefix = AppStringsPremium.taxPlanningNarrativeTdsLabel
    val dsopPrefix = AppStringsPremium.taxPlanningNarrativeDsopLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = AppStringsPremium.taxPlanningNarrativeYtdTotalLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$tdsPrefix$formattedTds  |  $dsopPrefix$formattedDsop",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
