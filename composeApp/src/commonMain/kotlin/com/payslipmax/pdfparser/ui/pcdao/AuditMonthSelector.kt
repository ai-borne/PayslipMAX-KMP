package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.timeline.VaultMonthGroupMapper
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/**
 * Prominent full-width Month Picker matching the Insights Screen architecture.
 * Serves as the primary Single Source of Truth selector for PayslipMax AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelectorRow(
    availablePayslips: List<ParsedPayslip>,
    selectedPayslip: ParsedPayslip?,
    onSelect: (ParsedPayslip) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availablePayslips.isEmpty() && selectedPayslip == null) return

    var dropdownExpanded by remember { mutableStateOf(false) }
    val isInteractive = availablePayslips.size > 1
    val displayText = selectedPayslip?.let { "${it.monthName} ${it.year}" } ?: "Select Month"
    val sorted = remember(availablePayslips) { VaultMonthGroupMapper.sortNewestFirst(availablePayslips) }

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded && isInteractive,
        onExpandedChange = { if (isInteractive) dropdownExpanded = it },
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        MonthFilterChip(
            displayText = displayText,
            isInteractive = isInteractive,
            dropdownExpanded = dropdownExpanded,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, isInteractive),
        )
        if (isInteractive) {
            MonthDropdownMenu(
                expanded = dropdownExpanded,
                sorted = sorted,
                onDismiss = { dropdownExpanded = false },
                onSelect = {
                    onSelect(it)
                    dropdownExpanded = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthFilterChip(
    displayText: String,
    isInteractive: Boolean,
    dropdownExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = true,
        onClick = {},
        label = {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = AppDimensions.SpacingMedium),
            )
        },
        trailingIcon = {
            if (isInteractive) {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
            }
        },
        colors = monthSelectorChipColors(),
        modifier = modifier.fillMaxWidth().testTag("pcdao_month_selector_chip"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.MonthDropdownMenu(
    expanded: Boolean,
    sorted: List<ParsedPayslip>,
    onDismiss: () -> Unit,
    onSelect: (ParsedPayslip) -> Unit,
) {
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        sorted.forEach { payslip ->
            DropdownMenuItem(
                text = { Text("${payslip.monthName} ${payslip.year}") },
                onClick = { onSelect(payslip) },
            )
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
