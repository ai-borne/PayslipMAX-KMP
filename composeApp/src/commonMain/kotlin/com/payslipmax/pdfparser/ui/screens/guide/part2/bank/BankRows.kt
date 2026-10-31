package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.BankRow
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.BankView
import com.payslipmax.pdfparser.ui.screens.guide.GuideTileFrame
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.BankColors
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** The status colour, from the theme tokens only. */
@Composable
internal fun statusTint(status: BankStatus): Color = if (status == BankStatus.NOT_ATTACHED) BankColors.waiting() else BankColors.attached()

/** The three chips, each with the count behind it (always over the whole Bank). */
@Composable
internal fun BankChips(
    view: BankView,
    selected: BankFilter,
    onSelect: (BankFilter) -> Unit,
) {
    val counts = mapOf(BankFilter.ALL to view.all, BankFilter.NOT_ATTACHED to view.notAttached, BankFilter.ATTACHED to view.attached)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        BankFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(OrdersBankStrings.chipWithCount(filter, counts.getValue(filter))) },
            )
        }
    }
}

/** One entry in the list: the HRMS code, the manual's name, From (and To), the status label and, while waiting, the age. */
@Composable
internal fun BankRowView(
    row: BankRow,
    name: String?,
    onClick: () -> Unit,
) {
    val entry = row.entry
    GuideTileFrame(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            Text(entry.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            name?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(dateRange(entry.from, entry.to), style = MaterialTheme.typography.bodyMedium)
            Text(OrdersBankStrings.status(row.status), style = MaterialTheme.typography.labelLarge, color = statusTint(row.status), fontWeight = FontWeight.SemiBold)
            row.age?.let { Text(OrdersBankStrings.waiting(it), style = MaterialTheme.typography.labelMedium, color = statusTint(row.status)) }
        }
    }
}

private fun dateRange(
    from: CalendarDate,
    to: CalendarDate?,
): String = PreClaimDates.format(from) + (to?.let { " – ${PreClaimDates.format(it)}" } ?: "")
