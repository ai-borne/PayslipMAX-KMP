package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings

/**
 * The leave rows as one statement prints them (Date · Type · Days · Total); tapping a row shows its Part II order. Shared by the
 * Retirement Calculators card and the payslip view. Which rows are open survives process death; the rows are keyed by the table,
 * so a different statement starts with every row closed.
 */
@Composable
fun LeaveAccumulationTable(
    rows: List<LeaveRowUi>,
    modifier: Modifier = Modifier,
) {
    // IntArray, not a Set: a Bundle can store it, so rememberSaveable keeps it across process death.
    var openRows by rememberSaveable(rows) { mutableStateOf(intArrayOf()) }

    Column(modifier = modifier.fillMaxWidth()) {
        LeaveTableRow(
            cells = listOf(LeaveBankStrings.columnDate, LeaveBankStrings.columnType, LeaveBankStrings.columnDays, LeaveBankStrings.columnTotal),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        rows.forEachIndexed { index, row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val open = index in openRows
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { openRows = if (open) openRows.filter { it != index }.toIntArray() else openRows + index },
            ) {
                LeaveTableRow(cells = listOf(row.date, row.type, row.days, row.total), style = MaterialTheme.typography.bodySmall)
                if (open) {
                    Text(
                        text = row.order,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = AppDimensions.SpacingSmall),
                    )
                }
            }
        }
    }
}

/** Date and Type take the room; Days and Total are short numbers, right-aligned so the columns line up. */
@Composable
private fun LeaveTableRow(
    cells: List<String>,
    style: TextStyle,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Cell(cells[0], style, TEXT_COLUMN_WEIGHT, TextAlign.Start)
        Cell(cells[1], style, TEXT_COLUMN_WEIGHT, TextAlign.Start)
        Cell(cells[2], style, NUMBER_COLUMN_WEIGHT, TextAlign.End)
        Cell(cells[3], style, NUMBER_COLUMN_WEIGHT, TextAlign.End)
    }
}

@Composable
private fun RowScope.Cell(
    text: String,
    style: TextStyle,
    weight: Float,
    align: TextAlign,
) {
    Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurface, textAlign = align, modifier = Modifier.weight(weight))
}

private const val TEXT_COLUMN_WEIGHT = 1.3f
private const val NUMBER_COLUMN_WEIGHT = 0.6f
