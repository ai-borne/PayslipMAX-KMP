package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.Opportunity
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

@Composable
fun TaxActionableChecklistCard(
    opportunities: List<Opportunity>,
    modifier: Modifier = Modifier,
) {
    if (opportunities.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        opportunities.forEach { opp ->
            FlatBorderedCard {
                // Single child: the outer FlatBorderedCard's inter-child spacing never applies, so
                // this inner Column's manual Spacers control gaps exactly as before extraction.
                Column {
                    Text(
                        text = opp.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                    Text(
                        text = opp.action,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (opp.estTaxSaved > 0) {
                        Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
                        val formattedSaved = TaxLedgerAggregator.formatIndianCurrency(opp.estTaxSaved)
                        Text(
                            text = "${InsightsStrings.taxPlanningEstTaxSaved}$formattedSaved",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
