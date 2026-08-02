package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.insights.TdsRunwayResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun TaxTdsRunwayProgressCard(
    tdsRunway: TdsRunwayResult,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsPremium.taxPlanningTdsRunwayTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TdsRunwayMetricsAndProgress(tdsRunway = tdsRunway)
        if (tdsRunway.hasTdsSpikeWarning) {
            TdsSpikeAlertBanner(currentTds = tdsRunway.currentMonthlyTds, projectedTds = tdsRunway.projectedMonthlyTds)
        }
    }
}

@Composable
private fun TdsRunwayMetricsAndProgress(
    tdsRunway: TdsRunwayResult,
) {
    val ytdText = TaxLedgerAggregator.formatIndianCurrency(tdsRunway.ytdTdsDeducted)
    val remText = TaxLedgerAggregator.formatIndianCurrency(tdsRunway.remainingTaxPayable)
    val projText = TaxLedgerAggregator.formatIndianCurrency(tdsRunway.projectedMonthlyTds)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text(
            text = "${AppStringsPremium.taxPlanningTdsPaidYtd}$ytdText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${AppStringsPremium.taxPlanningTdsRemaining}$remText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    val progress =
        if (tdsRunway.totalAnnualTaxLiability > 0) {
            (tdsRunway.ytdTdsDeducted / tdsRunway.totalAnnualTaxLiability).toFloat().coerceIn(0f, 1f)
        } else {
            1f
        }

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(AppDimensions.SpacingTiny),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Text(
        text = "${AppStringsPremium.taxPlanningMonthlyTdsRunway}$projText/mo (${tdsRunway.remainingMonths} mos remaining)",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun TdsSpikeAlertBanner(
    currentTds: Double,
    projectedTds: Double,
) {
    val currText = TaxLedgerAggregator.formatIndianCurrency(currentTds)
    val projText = TaxLedgerAggregator.formatIndianCurrency(projectedTds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = AppStringsPremium.taxPlanningTdsSpikeAlertTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "${AppStringsPremium.taxPlanningTdsSpikeAlertDesc}$currText to $projText per month in remaining FY!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
