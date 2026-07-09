package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.insights.WealthOptimizationEngine
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

data class TaxOptViewItem(
    val title: String,
    val action: String,
    val unusedAmount: Double,
    val estTaxSaved: Double,
    val regimeLabel: String,
)

fun buildTaxOptViewItems(optimizationResult: OptimizationResult): List<TaxOptViewItem> =
    optimizationResult.opportunities.map { opp ->
        TaxOptViewItem(
            title = opp.title,
            action = opp.action,
            unusedAmount = opp.unusedAmount,
            estTaxSaved = opp.estTaxSaved,
            regimeLabel = InsightsStrings.taxPlanningOldRegimeEst,
        )
    }

@Composable
fun TaxPlanningScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    val tax = selected?.taxAndSavings
    val optimizationResult = remember(selected) { selected?.let { WealthOptimizationEngine.analyze(it) } }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        ScreenBackHeader(title = AppStringsPremium.taxPlanningTitle, subtitle = AppStringsPremium.taxPlanningSubtitle, onBack = onBack)
        if (tax != null && optimizationResult != null) {
            TaxSummaryCard(tax = tax)
            TaxOpportunitiesHeader()
            TaxOptimizationsList(optimizationResult = optimizationResult)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = AppStringsPremium.taxPlanningNoProjections,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TaxOpportunitiesHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text(
            text = AppStringsPremium.taxPlanningSavingsProjections,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = InsightsStrings.taxPlanningRegimeDisclaimer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TaxSummaryCard(
    tax: TaxAndSavings,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            TaxMetricsRow(label = AppStringsPremium.taxPlanningGrossYtd, value = tax.grossSalaryYtd)
            TaxMetricsRow(label = AppStringsPremium.taxPlanningTaxable, value = tax.netTaxableIncome)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            TaxMetricsRow(label = AppStringsPremium.taxPlanningITax, value = tax.totalTaxPayable, isHighlight = true)
        }
    }
}

@Composable
private fun TaxMetricsRow(
    label: String,
    value: Double,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "₹${value.toInt()}",
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TaxOptimizationsList(
    optimizationResult: OptimizationResult,
    modifier: Modifier = Modifier,
) {
    val items = remember(optimizationResult) { buildTaxOptViewItems(optimizationResult) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        items.forEach { item -> TaxOptCard(item = item) }
    }
}

@Composable
private fun TaxOptCard(
    item: TaxOptViewItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
            Text(
                text = item.action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${InsightsStrings.taxPlanningHeadroom}${item.unusedAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${InsightsStrings.taxPlanningEstTaxSaved}${item.estTaxSaved.toInt()} ${item.regimeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
