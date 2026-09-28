package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCard
import com.payslipmax.pdfparser.ui.screens.formatCurrency

/**
 * Finding card for one detected audit discrepancy with side-by-side math table and authority citation.
 */
@Composable
fun AuditDiscrepancyCard(
    discrepancy: AuditDiscrepancy,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(
        modifier = modifier.fillMaxWidth(),
        tint = CardTint.Neutral,
        contentSpacing = 6.dp,
    ) {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CardHeader(discrepancy = discrepancy)
            Text(
                text = discrepancy.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MathDiffTable(discrepancy = discrepancy)
            RemediationAndAuthority(discrepancy = discrepancy)
        }
    }
}

@Composable
private fun CardHeader(discrepancy: AuditDiscrepancy) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = discrepancy.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SeverityBadge(severity = discrepancy.severity)
            TypeBadge(type = discrepancy.type)
        }
    }
}

@Composable
private fun SeverityBadge(severity: DiscrepancySeverity) {
    val (color, text) =
        when (severity) {
            DiscrepancySeverity.CRITICAL -> Color(0xFFEF4444) to "CRITICAL"
            DiscrepancySeverity.WARNING -> Color(0xFFFBBF24) to "WARNING"
            DiscrepancySeverity.INFO -> Color(0xFF38BDF8) to "INFO"
        }
    Box(
        modifier =
            Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TypeBadge(type: DiscrepancyType) {
    val (color, text) =
        when (type) {
            DiscrepancyType.UNDERPAYMENT -> Color(0xFF34D399) to "DUE"
            DiscrepancyType.RECOVERY_HAZARD -> Color(0xFFF87171) to "HAZARD"
            DiscrepancyType.FORFEITURE_RISK -> Color(0xFFFBBF24) to "ALARM"
            DiscrepancyType.TAX_EXPOSURE -> Color(0xFFC084FC) to "TAX"
        }
    Box(
        modifier =
            Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MathDiffTable(discrepancy: AuditDiscrepancy) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = AppStringsPcdao.colEntitled, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = formatCurrency(discrepancy.entitledAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Column {
            Text(text = AppStringsPcdao.colCredited, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = formatCurrency(discrepancy.drawnAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = AppStringsPcdao.colNetDue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val netColor = if (discrepancy.netDue >= 0) Color(0xFF34D399) else Color(0xFFF87171)
            Text(text = formatCurrency(discrepancy.netDue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = netColor)
        }
    }
}

@Composable
private fun RemediationAndAuthority(discrepancy: AuditDiscrepancy) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "💡 ${discrepancy.recommendedAction}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${AppStringsPcdao.authorityPrefix}${discrepancy.authority}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}
