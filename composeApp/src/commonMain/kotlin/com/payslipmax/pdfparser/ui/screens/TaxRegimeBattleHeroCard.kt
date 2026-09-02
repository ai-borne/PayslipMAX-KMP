package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.RegimeComparisonResult
import com.payslipmax.pdfparser.insights.RegimeTaxDetail
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

@Composable
fun TaxRegimeBattleHeroCard(
    comparison: RegimeComparisonResult,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, tint = CardTint.Accent, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsPremium.taxPlanningRegimeBattleTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            RegimeOptionBox(
                detail = comparison.oldRegime,
                isWinner = comparison.winnerRegime == "OLD",
                savings = if (comparison.winnerRegime == "OLD") comparison.annualSavings else 0.0,
                modifier = Modifier.weight(1f),
            )
            RegimeOptionBox(
                detail = comparison.newRegime,
                isWinner = comparison.winnerRegime == "NEW",
                savings = if (comparison.winnerRegime == "NEW") comparison.annualSavings else 0.0,
                modifier = Modifier.weight(1f),
            )
        }

        if (comparison.breakEvenDeduction > 0) {
            BreakEvenDeductionGap(comparison = comparison)
        }
    }
}

/**
 * D9: the break-even figure and the user's actual (capped) old-regime deductions are rendered
 * together from the same [comparison] so they can never diverge into two disconnected numbers again.
 */
@Composable
private fun BreakEvenDeductionGap(
    comparison: RegimeComparisonResult,
) {
    val currentDeductions = comparison.oldRegime.totalDeductionsAndExemptions - comparison.oldRegime.standardDeduction
    val gap = maxOf(0.0, comparison.breakEvenDeduction - currentDeductions)

    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(
            text = "${AppStringsPremium.taxPlanningBreakEvenText}${TaxLedgerAggregator.formatIndianCurrency(comparison.breakEvenDeduction)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${AppStringsPremium.taxPlanningCurrentDeductionsText}${TaxLedgerAggregator.formatIndianCurrency(currentDeductions)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (gap > 0) {
            Text(
                text = "${AppStringsPremium.taxPlanningDeductionGapText}${TaxLedgerAggregator.formatIndianCurrency(gap)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RegimeOptionBox(
    detail: RegimeTaxDetail,
    isWinner: Boolean,
    savings: Double,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        color = if (isWinner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border =
            BorderStroke(
                width = if (isWinner) AppDimensions.BorderMedium else AppDimensions.BorderThin,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            RegimeOptionHeader(regimeName = detail.regimeName, isWinner = isWinner)
            RegimeOptionAmounts(detail = detail, isWinner = isWinner, savings = savings)
        }
    }
}

@Composable
private fun RegimeOptionHeader(
    regimeName: String,
    isWinner: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (regimeName == "OLD") AppStringsPremium.taxPlanningOldRegimeLabel else AppStringsPremium.taxPlanningNewRegimeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isWinner) {
            Text(
                text = AppStringsTaxPlanner.bestBadge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** D17/D14: base tax, surcharge and cess are now shown explicitly, not folded silently into the total. */
@Composable
private fun RegimeOptionAmounts(
    detail: RegimeTaxDetail,
    isWinner: Boolean,
    savings: Double,
) {
    val formattedTax = TaxLedgerAggregator.formatIndianCurrency(detail.totalTaxPayable)
    Text(
        text = "${AppStringsTaxPlanner.rupeeSymbol}$formattedTax",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )

    Text(
        text = "${AppStringsTaxPlanner.regimeBaseTaxLabel}${TaxLedgerAggregator.formatIndianCurrency(detail.baseTax)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (detail.surcharge > 0) {
        Text(
            text = "${AppStringsTaxPlanner.regimeSurchargeLabel}${TaxLedgerAggregator.formatIndianCurrency(detail.surcharge)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        text = "${AppStringsTaxPlanner.regimeCessLabel}${TaxLedgerAggregator.formatIndianCurrency(detail.cess)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (savings > 0) {
        val formattedSavings = TaxLedgerAggregator.formatIndianCurrency(savings)
        Text(
            text = "${AppStringsPremium.taxPlanningWinnerSavingsBadge}$formattedSavings",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
