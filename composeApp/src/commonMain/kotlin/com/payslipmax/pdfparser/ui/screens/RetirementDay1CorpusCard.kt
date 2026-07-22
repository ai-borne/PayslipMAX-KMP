package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.RetirementPlannerResult
import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun RetirementDay1CorpusCard(
    result: RetirementPlannerResult,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = AppStringsPremium.retDay1CorpusTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            CorpusRow("DSOP Fund Balance", result.dsopBalance, "Tax-Free Final Balance")
            CorpusRow("Retirement Gratuity", result.retirementGratuity, PensionRuleKnowledgeBase.TaxExemptions.GRATUITY_SECTION)
            CorpusRow("Leave Encashment (300 Days)", result.leaveEncashment, PensionRuleKnowledgeBase.TaxExemptions.LEAVE_ENCASHMENT_SECTION)
            CorpusRow("AGIF Maturity Payout", result.agifMaturity, AppStringsPremium.retAgifNote)
            CorpusRow("Commuted Lump Sum (50%)", result.commutedLumpSum50, PensionRuleKnowledgeBase.TaxExemptions.COMMUTATION_SECTION)
        }
    }
}

@Composable
private fun CorpusRow(
    title: String,
    amount: Double,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
