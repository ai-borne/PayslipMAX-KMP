package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.InsightsStrings
import kotlin.math.abs

data class FindingItem(
    val isPositive: Boolean,
    val text: String,
)

data class HighlightItem(
    val text: String,
)

@Composable
fun KeyFindingsSection(
    state: InsightsState,
    modifier: Modifier = Modifier,
) {
    val findings = remember(state) { calculateKeyFindings(state) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = InsightsStrings.keyFindingsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            findings.forEach { finding ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
                ) {
                    Text(if (finding.isPositive) "✅" else "⚠️")
                    Text(text = finding.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AiHighlightsSection(
    state: InsightsState,
    modifier: Modifier = Modifier,
) {
    val highlights = remember(state) { calculateAiHighlights(state) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = InsightsStrings.aiHighlightsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            highlights.forEach { highlight ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
                ) {
                    Text("•")
                    Text(text = highlight.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun calculateKeyFindings(state: InsightsState): List<FindingItem> {
    val findings = mutableListOf<FindingItem>()
    val current = state.currentRecord
    val previous = state.previousRecord ?: return listOf(FindingItem(true, "First statement uploaded - findings will update next month."))

    // Net Salary Change
    val netDiff = current.netPay - previous.netPay
    if (netDiff > 1.0) {
        findings.add(FindingItem(true, "Salary increased by ${formatCurrency(netDiff)}"))
    } else if (netDiff < -1.0) {
        findings.add(FindingItem(false, "Salary decreased by ${formatCurrency(abs(netDiff))}"))
    } else {
        findings.add(FindingItem(true, "Net Salary stable"))
    }

    // Income Tax Change
    val taxDiff = current.incomeTax - previous.incomeTax
    if (taxDiff > 10.0 && previous.incomeTax > 0.0) {
        val pct = ((taxDiff / previous.incomeTax) * 100).toInt()
        findings.add(FindingItem(false, "Income Tax increased by $pct%"))
    } else if (taxDiff < -10.0 && previous.incomeTax > 0.0) {
        val pct = ((abs(taxDiff) / previous.incomeTax) * 100).toInt()
        findings.add(FindingItem(true, "Income Tax decreased by $pct%"))
    } else {
        findings.add(FindingItem(true, "Income Tax stable"))
    }

    // DSOP Stability
    if (abs(current.dsopSubscription - previous.dsopSubscription) <= 1.0) {
        findings.add(FindingItem(true, "DSOP contribution stable"))
    } else {
        findings.add(FindingItem(true, "DSOP contribution adjusted"))
    }

    // Missing Allowances Anomaly Check
    val missingAnomalies = state.engineResult.anomalies.filter { it.type == "MISSING_ALLOWANCE" }
    missingAnomalies.forEach { anomaly ->
        findings.add(FindingItem(false, "${anomaly.field.replaceFirstChar { it.uppercase() }} missing"))
    }

    return findings
}

fun calculateAiHighlights(state: InsightsState): List<HighlightItem> {
    val highlights = mutableListOf<HighlightItem>()
    val current = state.currentRecord

    // Largest Deduction
    val tax = current.incomeTax
    val dsop = current.dsopSubscription
    if (tax > dsop && tax > 0.0) {
        highlights.add(HighlightItem("Largest deduction this month: Income Tax (${formatCurrency(tax)})"))
    } else if (dsop > 0.0) {
        highlights.add(HighlightItem("Largest deduction this month: DSOP Subscription (${formatCurrency(dsop)})"))
    }

    // Biggest Salary Change & Allowance Movement (Local Calculations)
    val previous = state.previousRecord
    if (previous != null) {
        val basicDiff = current.basicPay - previous.basicPay
        val daDiff = current.dearnessAllowance - previous.dearnessAllowance
        val tptaDiff = current.transportAllowance - previous.transportAllowance
        val hraDiff = current.houseRentAllowance - previous.houseRentAllowance

        val maxChange =
            listOf(
                "Basic Pay" to basicDiff,
                "Dearness Allowance" to daDiff,
                "Transport Allowance" to tptaDiff,
                "House Rent Allowance" to hraDiff,
            ).maxByOrNull { abs(it.second) }

        if (maxChange != null && abs(maxChange.second) > 10.0) {
            val direction = if (maxChange.second > 0) "increase" else "decrease"
            highlights.add(HighlightItem("Biggest component change: ${maxChange.first} ($direction of ${formatCurrency(abs(maxChange.second))})"))
        }

        // Notable Allowance Movement
        if (abs(daDiff) > 10.0 && maxChange?.first != "Dearness Allowance") {
            highlights.add(HighlightItem("Notable allowance movement: Dearness Allowance changed by ${formatCurrency(abs(daDiff))}"))
        } else if (abs(hraDiff) > 10.0 && maxChange?.first != "House Rent Allowance") {
            highlights.add(HighlightItem("Notable allowance movement: House Rent Allowance changed by ${formatCurrency(abs(hraDiff))}"))
        }
    }

    if (highlights.isEmpty()) {
        highlights.add(HighlightItem("Stable financial components compared to last month."))
    }

    return highlights
}
