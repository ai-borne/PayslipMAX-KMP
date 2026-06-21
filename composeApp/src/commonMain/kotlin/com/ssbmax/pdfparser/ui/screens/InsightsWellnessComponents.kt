package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.InsightsStrings
import kotlin.math.abs

// ── Top bar: month selector + wellness chip ──────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsTopBar(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip,
    onSelectPayslip: (ParsedPayslip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = AppDimensions.SpacingTiny,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.SpacingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthSelectorDropdown(
                payslips = payslips,
                selected = selected,
                onSelectPayslip = onSelectPayslip,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthSelectorDropdown(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip,
    onSelectPayslip: (ParsedPayslip) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val sorted =
        remember(payslips) {
            payslips.sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum }).reversed()
        }
    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = it },
        modifier = modifier,
    ) {
        FilterChip(
            selected = true,
            onClick = {},
            label = {
                Text(
                    text = "${selected.monthName} ${selected.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
        ) {
            sorted.forEach { payslip ->
                DropdownMenuItem(
                    text = { Text("${payslip.monthName} ${payslip.year}") },
                    onClick = {
                        onSelectPayslip(payslip)
                        dropdownExpanded = false
                    },
                )
            }
        }
    }
}

// ── Delta formatting (shared by HealthKpiCard) ───────────────────────────────

fun getDeltaText(delta: Int?) =
    when {
        delta == null || delta == 0 -> ""
        delta > 0 -> " ▲$delta"
        else -> " ▼${abs(delta)}"
    }

// ── Wellness driver row (reused by HealthKpiCard breakdown) ─────────────────

@Composable
fun WellnessDriverRow(driver: WellnessDriver) {
    val impactColor =
        when {
            driver.pointImpact > 0 -> MaterialTheme.colorScheme.primary
            driver.pointImpact < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val impactSign =
        when {
            driver.pointImpact > 0 -> "+${driver.pointImpact}"
            driver.pointImpact == 0 -> "±0"
            else -> "${driver.pointImpact}"
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = driver.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = impactSign,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = impactColor,
            )
        }
        if (driver.improvePath != null) {
            Text(
                text = "${InsightsStrings.wellnessImprovePathPrefix}${driver.improvePath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
