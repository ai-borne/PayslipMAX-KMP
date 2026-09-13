package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.WellnessDriver
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import kotlin.math.abs

// ── Top bar: hero title/subtext + month selector ─────────────────────────────
// Pay Health now surfaces only as the expandable chip inside MonthlySnapshot (D-approved: single
// surface for the score) — the top-bar pill was removed in the Phase 4 redesign wiring.
// Plain Column on the screen background, matching HistoryHeader/Settings — no elevated Surface
// wrapper, so this reads as the screen's own header rather than a distinct ribbon/card.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsTopBar(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip,
    onSelectPayslip: (ParsedPayslip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.SpacingSmall),
    ) {
        Text(
            text = AppStrings.navigationInsights,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = InsightsStrings.heroSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
        MonthSelectorDropdown(
            payslips = payslips,
            selected = selected,
            onSelectPayslip = onSelectPayslip,
            modifier = Modifier.fillMaxWidth(),
        )
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = AppDimensions.SpacingMedium),
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            colors = monthSelectorChipColors(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
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

@Composable
private fun monthSelectorChipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor =
            if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
            },
        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
    )

// ── Delta formatting (shared by MonthlySnapshot's Pay Health chip) ──────────

fun getTrendText(
    delta: Int?,
    previousMonthLabel: String?,
): String {
    if (delta == null || delta == 0) return ""
    val sinceLabel = previousMonthLabel ?: InsightsStrings.wellnessTrendSinceLastPayslip
    return if (delta > 0) {
        "${InsightsStrings.wellnessTrendImprovedPrefix} $delta ${InsightsStrings.wellnessTrendPointsSince} $sinceLabel"
    } else {
        "${InsightsStrings.wellnessTrendDownPrefix} ${abs(delta)} ${InsightsStrings.wellnessTrendPointsSince} $sinceLabel"
    }
}

// ── Wellness driver row (reused by MonthlySnapshot's Pay Health chip breakdown) ─

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
            val titlePrefix =
                if (driver.pointImpact >= 0) {
                    InsightsStrings.wellnessPositiveDriverPrefix
                } else {
                    InsightsStrings.wellnessWatchDriverPrefix
                }
            Text(
                text = "$titlePrefix${driver.title}",
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
