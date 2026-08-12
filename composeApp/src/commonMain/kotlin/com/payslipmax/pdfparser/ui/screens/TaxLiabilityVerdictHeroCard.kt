package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.RegimeComparisonResult
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.insights.TdsRunwayResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

/**
 * D15 replacement: the deleted Peer Benchmark card compared the user to himself and could not
 * inform. This hero answers the three questions the screen actually owes an opening answer to --
 * what will I owe, which regime, and what changes next month.
 */
@Composable
fun TaxLiabilityVerdictHeroCard(
    regimeComparison: RegimeComparisonResult,
    tdsRunway: TdsRunwayResult?,
    projectedNextMonthNetPay: Double?,
    parsedMonthCount: Int,
    modifier: Modifier = Modifier,
) {
    val isNewWinner = regimeComparison.winnerRegime == "NEW"
    val winningDetail = if (isNewWinner) regimeComparison.newRegime else regimeComparison.oldRegime
    val regimeLabel = if (isNewWinner) AppStringsPremium.taxPlanningNewRegimeLabel else AppStringsPremium.taxPlanningOldRegimeLabel

    FlatBorderedCard(tint = CardTint.Accent, modifier = modifier, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsTaxPlanner.heroTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LiabilityVerdictAmount(totalTax = winningDetail.totalTaxPayable, parsedMonthCount = parsedMonthCount)

        Text(
            text = "${AppStringsTaxPlanner.heroRegimeWinnerPrefix}$regimeLabel${AppStringsTaxPlanner.heroRegimeWinnerSuffix}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        NextMonthOutlookRow(tdsRunway = tdsRunway, projectedNextMonthNetPay = projectedNextMonthNetPay)
    }
}

@Composable
private fun NextMonthOutlookRow(
    tdsRunway: TdsRunwayResult?,
    projectedNextMonthNetPay: Double?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tdsRunway?.let {
            Text(
                text = "${AppStringsTaxPlanner.heroNextMonthTdsLabel}${TaxLedgerAggregator.formatIndianCurrency(it.projectedMonthlyTds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        projectedNextMonthNetPay?.let {
            Text(
                text = "${AppStringsTaxPlanner.heroNextMonthNetPayLabel}${TaxLedgerAggregator.formatIndianCurrency(it)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Precision (D17): a projection built from 1-2 months of data is not rupee-exact -- round to the
 * nearest thousand and, below [LOW_COVERAGE_MONTHS], show it as a range rather than a false-precision point.
 */
@Composable
private fun LiabilityVerdictAmount(
    totalTax: Double,
    parsedMonthCount: Int,
) {
    val rounded = roundToNearestThousand(totalTax)
    if (parsedMonthCount in 1 until LOW_COVERAGE_MONTHS) {
        val lower = roundToNearestThousand(totalTax * (1.0 - LOW_COVERAGE_BAND))
        val upper = roundToNearestThousand(totalTax * (1.0 + LOW_COVERAGE_BAND))
        Text(
            text =
                "${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(lower)}" +
                    "${AppStringsTaxPlanner.heroRangeSeparator}${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(upper)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${AppStringsTaxPlanner.heroLowCoveragePrefix}$parsedMonthCount${AppStringsTaxPlanner.heroLowCoverageSuffix}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = "${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(rounded)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStringsTaxPlanner.heroLiabilityLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val LOW_COVERAGE_MONTHS = 3
private const val LOW_COVERAGE_BAND = 0.08

private fun roundToNearestThousand(amount: Double): Double = kotlin.math.round(amount / 1000.0) * 1000.0
