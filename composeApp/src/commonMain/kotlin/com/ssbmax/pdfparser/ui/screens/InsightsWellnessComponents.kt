package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.ssbmax.pdfparser.ui.theme.AppStrings
import kotlin.math.abs

// ── Top bar: month selector + wellness chip ──────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsTopBar(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip,
    score: Int,
    delta: Int?,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    onSelectPayslip: (ParsedPayslip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = AppDimensions.SpacingTiny,
    ) {
        Row(
            modifier = Modifier
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
            WellnessChip(
                score = score,
                delta = delta,
                expanded = expanded,
                onExpandClick = onExpandClick,
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
    val sorted = remember(payslips) {
        payslips.sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum }).reversed()
    }
    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = it },
        modifier = modifier,
    ) {
        FilterChip(
            selected = true,
            onClick = { dropdownExpanded = !dropdownExpanded },
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

// ── Wellness chip (compact, tappable) ────────────────────────────────────────

@Composable
fun WellnessChip(
    score: Int,
    delta: Int?,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = when {
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 50 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    val deltaText = when {
        delta == null || delta == 0 -> ""
        delta > 0 -> " ▲$delta"
        else -> " ▼${abs(delta)}"
    }
    Surface(
        modifier = modifier.clickable(onClick = onExpandClick),
        shape = RoundedCornerShape(AppDimensions.IconSizeHuge),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimensions.PaddingSmall,
                vertical = AppDimensions.SpacingTiny,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = AppStrings.wellnessChipLabel,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
            Text(
                text = "$score$deltaText",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

// ── Wellness drivers panel (expanded section) ────────────────────────────────

@Composable
fun WellnessDriversSection(
    score: Int,
    drivers: List<WellnessDriver>,
    modifier: Modifier = Modifier,
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = AppStrings.wellnessDriversTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "$score/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            drivers.forEach { driver -> WellnessDriverRow(driver = driver) }
        }
    }
}

@Composable
private fun WellnessDriverRow(driver: WellnessDriver) {
    val impactColor = when {
        driver.pointImpact > 0 -> MaterialTheme.colorScheme.primary
        driver.pointImpact < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val impactSign = when {
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
                text = "→ ${driver.improvePath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
