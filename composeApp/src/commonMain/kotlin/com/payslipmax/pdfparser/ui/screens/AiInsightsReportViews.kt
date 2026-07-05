package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.*
import com.payslipmax.pdfparser.ui.theme.AppColors
import com.payslipmax.pdfparser.ui.theme.AppDimensions

@Composable
fun ParsedReportContent(report: AiInsightReport) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (report.salaryChanges.isNotEmpty()) {
            SalaryChangesView(report.salaryChanges)
        }
        if (report.missingAllowances.isNotEmpty()) {
            MissingAllowancesView(report.missingAllowances)
        }
        if (report.newDeductions.isNotEmpty()) {
            NewDeductionsView(report.newDeductions)
        }
        if (report.riskAlerts.isNotEmpty()) {
            RiskAlertsView(report.riskAlerts)
        }
        if (report.opportunities.isNotEmpty()) {
            OpportunitiesView(report.opportunities)
        }
        if (report.actionRequired.isNotEmpty()) {
            ActionRequiredView(report.actionRequired)
        }
    }
}

@Composable
private fun SalaryChangesView(changes: List<SalaryChangeItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text("📊 Salary Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            changes.forEach { change ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingTiny),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(change.item, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val color =
                        when (change.change) {
                            "increased" -> MaterialTheme.colorScheme.secondary
                            "decreased" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    val prefix =
                        when (change.change) {
                            "increased" -> "+"
                            "decreased" -> "-"
                            else -> ""
                        }
                    Text(
                        text = "$prefix₹${change.amount.toInt()}",
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingAllowancesView(allowances: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text("⚠️ Missing Allowances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            allowances.forEach { allowance ->
                Text("• Absent: $allowance (was present previously)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun NewDeductionsView(deductions: List<NewDeductionItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text("💸 Deduction Adjustments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            deductions.forEach { deduction ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingTiny),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(deduction.item, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val color =
                        when (deduction.change) {
                            "increased" -> MaterialTheme.colorScheme.error
                            "decreased" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    val prefix =
                        when (deduction.change) {
                            "increased" -> "+"
                            "decreased" -> "-"
                            else -> ""
                        }
                    Text(
                        text = "$prefix₹${deduction.amount.toInt()}",
                        color = color,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskAlertsView(alerts: List<RiskAlertItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text("🚨 Risk & Compliance Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Warning)
        alerts.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Warning.copy(alpha = 0.08f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Warning.copy(alpha = 0.3f))),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
                    Text(alert.observation, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                    Text("💡 Action: ${alert.action}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun OpportunitiesView(opportunities: List<OpportunityItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text("💡 Opportunities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        opportunities.forEach { opp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))),
            ) {
                Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
                    Text(opp.opportunity, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                    Text("🎯 Strategy: ${opp.action}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActionRequiredView(actions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text("✅ Required Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            actions.forEach { action ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingTiny),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("☑️", modifier = Modifier.padding(end = AppDimensions.SpacingSmall))
                    Text(action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
