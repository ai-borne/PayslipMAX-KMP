package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings

private const val MONTHS_PER_ROW = 4

/** Previous/next steppers around the current month; both skip months with no payslip because [months] only lists real ones. */
@Composable
fun PayAuditMonthBar(
    selected: PayMonth?,
    months: List<PayMonth>,
    onSelect: (PayMonth) -> Unit,
    onOpenPicker: () -> Unit,
    onOpenGlossary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = months.indexOf(selected)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(months[index - 1]) }, enabled = index > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = PayAuditVerdictStrings.previousMonthDescription)
        }
        FilledTonalButton(onClick = onOpenPicker, modifier = Modifier.weight(1f)) {
            Text(text = selected?.let(::formatPayMonth).orEmpty() + PayAuditVerdictStrings.dropdownSuffix, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { onSelect(months[index + 1]) }, enabled = index in 0 until months.lastIndex) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = PayAuditVerdictStrings.nextMonthDescription)
        }
        IconButton(onClick = onOpenGlossary) { Text(PayAuditVerdictStrings.infoGlyph, style = MaterialTheme.typography.titleLarge) }
    }
}

/** Grid of months for the years that have payslips; months without one are shown disabled, unlike Insights' calendar dropdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayAuditMonthPickerSheet(
    selected: PayMonth?,
    months: List<PayMonth>,
    onSelect: (PayMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(AppDimensions.PaddingMedium).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(PayAuditVerdictStrings.monthPickerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(PayAuditVerdictStrings.monthPickerNote, style = MaterialTheme.typography.bodySmall)
            months.map { it.year }.distinct().sortedDescending().forEach { year ->
                PayAuditYearGrid(year, months, selected, onSelect)
            }
        }
    }
}

@Composable
private fun PayAuditYearGrid(
    year: Int,
    months: List<PayMonth>,
    selected: PayMonth?,
    onSelect: (PayMonth) -> Unit,
) {
    Text(year.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    (1..PayslipPatternConfig.monthNames.lastIndex).chunked(MONTHS_PER_ROW).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            row.forEach { m ->
                val month = PayMonth(year, m)
                val has = month in months
                OutlinedButton(onClick = { onSelect(month) }, enabled = has, modifier = Modifier.weight(1f)) {
                    Text(PayslipPatternConfig.monthNames[m].take(3), fontWeight = if (month == selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
