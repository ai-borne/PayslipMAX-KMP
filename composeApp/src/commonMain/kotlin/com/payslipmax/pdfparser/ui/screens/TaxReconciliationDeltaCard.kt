package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.ArrearsTransparencyInsight
import com.payslipmax.pdfparser.insights.MidYearRegimeChangeInsight
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.insights.TaxTrackReconciliation
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

/**
 * ADR-3/ADR-4 (Phase 5 domain, Phase 6 UI wiring): the two-track model made visible -- what PCDA
 * will actually withhold vs what current law finally owes, plus the corollaries that only make
 * sense once that gap is on screen (a belated-return trap, a mid-year regime change, arrears
 * transparency). Every rendered sentence is generated in [TwoTrackReconciliationEngine] /
 * [RegimeDecisionPlanner] -- this card never re-derives numbers.
 */
@Composable
fun TaxReconciliationDeltaCard(
    reconciliation: TaxTrackReconciliation,
    belatedReturnTrapWarning: String?,
    midYearRegimeChange: MidYearRegimeChangeInsight?,
    arrearsTransparency: ArrearsTransparencyInsight?,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, contentSpacing = AppDimensions.SpacingSmall) {
        Text(
            text = AppStringsTaxPlanner.reconciliationCardTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            TrackFigure(
                label = AppStringsTaxPlanner.reconciliationTdsTrackLabel,
                amount = reconciliation.tdsTrackAnnual,
                regime = reconciliation.tdsTrackRegime.name,
                modifier = Modifier.weight(1f),
            )
            TrackFigure(
                label = AppStringsTaxPlanner.reconciliationLiabilityTrackLabel,
                amount = reconciliation.liabilityTrackAnnual,
                regime = reconciliation.liabilityTrackRegime.name,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = reconciliation.message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        arrearsTransparency?.let {
            ReconciliationNote(title = AppStringsTaxPlanner.arrearsTransparencyTitle, message = it.message)
        }
        belatedReturnTrapWarning?.let {
            ReconciliationNote(title = AppStringsTaxPlanner.belatedReturnWarningTitle, message = it, isWarning = true)
        }
        if (midYearRegimeChange?.detected == true) {
            midYearRegimeChange.message?.let {
                ReconciliationNote(title = AppStringsTaxPlanner.midYearChangeTitle, message = it, isWarning = true)
            }
        }
    }
}

@Composable
private fun TrackFigure(
    label: String,
    amount: Double,
    regime: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${AppStringsTaxPlanner.rupeeSymbol}${TaxLedgerAggregator.formatIndianCurrency(amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(text = regime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReconciliationNote(
    title: String,
    message: String,
    isWarning: Boolean = false,
) {
    val borderColor = if (isWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(AppDimensions.BorderThin, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
