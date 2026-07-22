package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.RetirementPlannerResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun CaTaxAuditCard(
    result: RetirementPlannerResult,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            CaTaxAuditHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            CaTaxAuditCorpusSection(taxFreeCorpus = result.taxFreeCorpus)
            CaTaxAuditMonthlySection(taxableMonthly = result.taxableMonthlyPension)
        }
    }
}

@Composable
private fun CaTaxAuditHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo)) {
        Text(
            text = AppStringsPremium.retCaTaxAuditTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStringsPremium.retCaTaxAuditSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaTaxAuditCorpusSection(taxFreeCorpus: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo)) {
        Text(
            text = AppStringsPremium.retCaTaxFreeCorpusLabel,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = formatCurrency(taxFreeCorpus),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStringsPremium.retCaTaxExemptNotes,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaTaxAuditMonthlySection(taxableMonthly: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo)) {
        Text(
            text = AppStringsPremium.retCaTaxableMonthlyLabel,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${formatCurrency(taxableMonthly)} / mo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
