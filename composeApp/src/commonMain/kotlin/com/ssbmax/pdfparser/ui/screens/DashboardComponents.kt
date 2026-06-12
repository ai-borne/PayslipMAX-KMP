package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfficerInfoBar(payslip: ParsedPayslip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = payslip.officer.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${AppStrings.cdaInfoLabel}: ${payslip.officer.accountNo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${AppStrings.panInfoLabel}: ${payslip.officer.pan}",
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YearDropdown(
                years = years,
                selectedYear = selectedYear,
                onYearSelected = { year ->
                    selectedYear = year
                    val monthsInYear = viewModel.getMonthsForYear(year)
                    val latestMonth = monthsInYear.firstOrNull()
                    if (latestMonth != null) {
                        selectedMonthNum = latestMonth.monthNum
                        viewModel.selectByYearMonth(year, latestMonth.monthNum)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            MonthDropdown(
                months = monthsForYear,
                selectedMonthNum = selectedMonthNum,
                onMonthSelected = { monthNum ->
                    selectedMonthNum = monthNum
                    viewModel.selectByYearMonth(selectedYear, monthNum)
                },
                modifier = Modifier.weight(1f),
            )
        }
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
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
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
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = months.find { it.monthNum == selectedMonthNum }?.monthName ?: ""
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
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
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

@Composable
internal fun EmptyStateScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "No Payslips Imported",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import your PDF payslips or seed simulated data from the import screen to view financial analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

internal fun formatAmount(amount: Double): String {
    val longVal = amount.toLong()
    if (longVal >= 100000) {
        val lakhs = longVal / 100000.0
        return "${lakhs.toString().take(4)}L"
    }
    val str = longVal.toString()
    if (str.length <= 3) return str
    val lastThree = str.substring(str.length - 3)
    val remaining = str.substring(0, str.length - 3)
    val builder = StringBuilder()
    var i = remaining.length
    while (i > 0) {
        if (i >= 2) {
            builder.insert(0, remaining.substring(i - 2, i))
            if (i - 2 > 0) builder.insert(0, ",")
            i -= 2
        } else {
            builder.insert(0, remaining.substring(0, 1))
            i -= 1
        }
    }
    return "$builder,$lastThree"
}
