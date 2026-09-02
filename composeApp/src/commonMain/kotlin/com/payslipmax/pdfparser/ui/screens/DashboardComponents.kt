package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfficerInfoBar(
    payslip: ParsedPayslip,
    profileName: String,
    profileCda: String,
    profilePan: String,
) {
    val displayName = if (profileName.isNotBlank()) profileName else payslip.officer.name
    val displayCda = if (profileCda.isNotBlank()) profileCda else payslip.officer.accountNo
    val displayPan = if (profilePan.isNotBlank()) profilePan else payslip.officer.pan

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (androidx.compose.foundation.isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    },
            ),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${AppStrings.cdaInfoLabel}: $displayCda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${AppStrings.panInfoLabel}: $displayPan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearMonthPickerRow(
    viewModel: PayslipViewModel,
    selected: ParsedPayslip?,
) {
    val years = viewModel.getAvailableYears()
    var selectedYear by remember(selected) {
        mutableStateOf(selected?.year ?: years.firstOrNull() ?: 2025)
    }
    val monthsForYear = viewModel.getMonthsForYear(selectedYear)
    var selectedMonthNum by remember(selected) {
        mutableStateOf(selected?.monthNum ?: monthsForYear.firstOrNull()?.monthNum ?: 1)
    }

    Column {
        Text(
            text = AppStrings.analyzingStatement,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            YearDropdown(
                years = years,
                selectedYear = selectedYear,
                onYearSelected = { year ->
                    selectYearInPicker(year, viewModel) { y, m ->
                        selectedYear = y
                        selectedMonthNum = m
                    }
                },
                modifier = Modifier.weight(1f),
            )
            MonthDropdown(
                months = monthsForYear,
                selectedMonthNum = selectedMonthNum,
                fallbackMonthName = if (selected?.year == selectedYear) selected.monthName else "",
                onMonthSelected = { monthNum ->
                    selectedMonthNum = monthNum
                    viewModel.selectByYearMonth(selectedYear, monthNum)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun selectYearInPicker(
    year: Int,
    viewModel: PayslipViewModel,
    onUpdated: (selectedYear: Int, selectedMonthNum: Int) -> Unit,
) {
    val monthsInYear = viewModel.getMonthsForYear(year)
    val latestMonth = monthsInYear.firstOrNull()
    if (latestMonth != null) {
        onUpdated(year, latestMonth.monthNum)
        viewModel.selectByYearMonth(year, latestMonth.monthNum)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearDropdown(
    years: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedYear.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(AppStrings.selectYearLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDropdown(
    months: List<ParsedPayslip>,
    selectedMonthNum: Int,
    fallbackMonthName: String = "",
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName =
        months.find { it.monthNum == selectedMonthNum }?.monthName
            ?: if (fallbackMonthName.isNotBlank()) fallbackMonthName else ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(AppStrings.selectMonthLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            months.forEach { payslip ->
                DropdownMenuItem(
                    text = { Text(payslip.monthName) },
                    onClick = {
                        onMonthSelected(payslip.monthNum)
                        expanded = false
                    },
                )
            }
        }
    }
}

internal fun formatAmount(amount: Double): String = FormatUtils.formatIndianCompact(amount)
