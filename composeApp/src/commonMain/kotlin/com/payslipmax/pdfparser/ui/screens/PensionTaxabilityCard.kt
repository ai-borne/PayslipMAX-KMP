package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase
import com.payslipmax.pdfparser.ui.theme.AppDimensions

@Composable
fun PensionTaxabilityCard(
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
                text = "🛡️ Taxability Rules for Defence Pensions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            TaxItem("Basic Service Pension", "Taxable under Salary Income head.", isTaxable = true)
            TaxItem("Disability Pension", PensionRuleKnowledgeBase.TaxExemptions.DISABILITY_PENSION_SECTION, isTaxable = false)
            TaxItem("Gallantry Pension", PensionRuleKnowledgeBase.TaxExemptions.GALLANTRY_PENSION_SECTION, isTaxable = false)
            TaxItem("Liberalised Family Pension", "100% Non-Taxable for Battle Casualties", isTaxable = false)
        }
    }
}

@Composable
private fun TaxItem(
    title: String,
    rule: String,
    isTaxable: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = rule,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
            color = if (isTaxable) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ) {
            Text(
                text = if (isTaxable) "Taxable" else "100% Tax Free",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isTaxable) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTwo),
            )
        }
    }
}
