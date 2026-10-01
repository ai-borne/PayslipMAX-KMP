package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.payslipmax.pdfparser.insights.timeline.PayFixationComparison
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.plusMonths
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings

/**
 * Pay-fixation option calculator (docs/Plan/09_PayAudit_PhasePlan.md Phase 6): the one Phase 6
 * deliverable that inherently needs a user input, because a future promotion cannot be read off the
 * timeline. Everything else — the officer's current level/stage and DNI cycle — is resolved from the
 * timeline by [resolveFixationComparison]; the user only picks the level they'd be promoted to and when.
 */
fun LazyListScope.payAuditFixationCalculatorItems(timeline: ServiceTimeline) {
    item(key = "pay_audit_fixation_header", contentType = "section_header") {
        Text(text = PayAuditStrings.fixationCalculatorTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    item(key = "pay_audit_fixation_calculator", contentType = "fixation_calculator") {
        if (timeline.months.any { it.level != null && it.stage != null }) {
            PayAuditFixationCalculatorCard(timeline)
        } else {
            Text(text = PayAuditStrings.fixationCalculatorEmptyState, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayAuditFixationCalculatorCard(timeline: ServiceTimeline) {
    // Defaults must resolve to a *valid* comparison against this officer's own timeline out of the box —
    // a hardcoded Level 11/January 2026 shows an error on first paint for anyone at Level 11+ already, or
    // whose latest payslip is already past January 2026 (resolveFixationComparison rejects both a toLevel
    // no higher than the current one and a promotion month not strictly after the latest trusted month).
    val latestMonth = remember(timeline) { timeline.months.maxByOrNull { it.month } }
    val defaultToLevel =
        remember(timeline) {
            latestMonth?.level?.let { current -> PayLevel.entries.getOrNull(current.ordinal + 1) } ?: PayLevel.L11
        }
    val defaultPromotionMonth = remember(timeline) { latestMonth?.month?.plusMonths(1) ?: PayMonth(2026, 1) }

    var toLevel by remember(timeline) { mutableStateOf(defaultToLevel) }
    var monthText by remember(timeline) { mutableStateOf(defaultPromotionMonth.month.toString()) }
    var yearText by remember(timeline) { mutableStateOf(defaultPromotionMonth.year.toString()) }
    var levelMenuExpanded by remember { mutableStateOf(false) }

    PredictionCard(title = PayAuditStrings.fixationCalculatorSubtitle) {
        ToLevelDropdown(toLevel, levelMenuExpanded, onExpandedChange = { levelMenuExpanded = it }, onLevelSelected = { toLevel = it })
        PromotionMonthYearFields(monthText, yearText, onMonthChange = { monthText = it }, onYearChange = { yearText = it })

        val month = monthText.toIntOrNull()?.coerceIn(1, 12)
        val year = yearText.toIntOrNull()
        val result =
            remember(timeline, toLevel, month, year) {
                if (month == null || year == null) {
                    null
                } else {
                    resolveFixationComparison(timeline, FixationCalculatorInputs(toLevel, year, month))
                }
            }

        if (result == null) {
            Text(text = PayAuditStrings.fixationCalculatorInvalidInput, style = MaterialTheme.typography.bodySmall)
        } else {
            if (result.dniMonthAssumed) {
                Text(
                    text = PayAuditStrings.fixationCalculatorDniAssumedNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PayFixationComparisonResult(result.comparison)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToLevelDropdown(
    toLevel: PayLevel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLevelSelected: (PayLevel) -> Unit,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = "${PayAuditStrings.fixationCalculatorToLevelLabel} ${toLevel.label}",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            PayLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.label) },
                    onClick = {
                        onLevelSelected(level)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun PromotionMonthYearFields(
    monthText: String,
    yearText: String,
    onMonthChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        OutlinedTextField(
            value = monthText,
            onValueChange = onMonthChange,
            label = { Text(PayAuditStrings.fixationCalculatorMonthLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.5f),
        )
        OutlinedTextField(
            value = yearText,
            onValueChange = onYearChange,
            label = { Text(PayAuditStrings.fixationCalculatorYearLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PayFixationComparisonResult(comparison: PayFixationComparison) {
    Text(
        text = "${PayAuditStrings.fixationCalculatorOption1Label}: ${formatCurrency(comparison.option1.fixedPay.toDouble())}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "${PayAuditStrings.fixationCalculatorOption2Label}: ${formatCurrency(comparison.option2.fixedPay.toDouble())}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "${PayAuditStrings.fixationCalculatorRecommendedPrefix}${comparison.recommendedOption}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
    )
}
