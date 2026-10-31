package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** What the Bank list needs from outside: who may add, and where each tap goes. */
internal class BankActions(
    val canAdd: Boolean,
    val onAdd: () -> Unit,
    val onUnlock: () -> Unit,
    val onOpenEntry: (String) -> Unit,
    val onFilter: (BankFilter) -> Unit,
    val onBack: () -> Unit,
)

/**
 * The Orders Bank: chips, then the entries newest first under year headings. An empty Bank says what it is for; a lapsed officer sees
 * everything and a note, but no Add that works (it offers Premium instead). The last backup is one line at the foot.
 */
@Composable
internal fun BankScreen(
    state: BankUiState,
    nameOf: (String) -> String?,
    actions: BankActions,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = OrdersBankStrings.title, onBack = actions.onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            item(key = "add") { AddButton(actions) }
            if (!actions.canAdd) item(key = "locked") { Text(OrdersBankStrings.lockedNote, style = MaterialTheme.typography.bodyMedium) }
            if (state.loaded) bankBody(state, nameOf, actions)
            item(key = "backup") { Text(backupLine(state.lastBackup), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun AddButton(actions: BankActions) {
    Button(onClick = if (actions.canAdd) actions.onAdd else actions.onUnlock, modifier = Modifier.fillMaxWidth()) {
        Text(if (actions.canAdd) OrdersBankStrings.addButton else OrdersBankStrings.upgrade)
    }
}

private fun LazyListScope.bankBody(
    state: BankUiState,
    nameOf: (String) -> String?,
    actions: BankActions,
) {
    if (state.unreadable > 0) item(key = "unreadable") { Text(OrdersBankStrings.unreadable(state.unreadable), style = MaterialTheme.typography.bodySmall) }
    if (state.view.all == 0) {
        item(key = "empty") { EmptyBank() }
        return
    }
    item(key = "chips") { BankChips(state.view, state.filter, actions.onFilter) }
    if (state.view.years.isEmpty()) item(key = "none") { Text(OrdersBankStrings.emptyFilter, style = MaterialTheme.typography.bodyMedium) }
    state.view.years.forEach { year ->
        item(key = "year-${year.year}") { Text(OrdersBankStrings.yearHeading(year.year), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(year.rows.size, key = { year.rows[it].entry.id }) { i ->
            val row = year.rows[i]
            BankRowView(row, nameOf(row.entry.code)) { actions.onOpenEntry(row.entry.id) }
        }
    }
}

@Composable
private fun EmptyBank() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(OrdersBankStrings.emptyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(OrdersBankStrings.emptyBody, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun backupLine(date: CalendarDate?): String = date?.let { OrdersBankStrings.backupLine(PreClaimDates.format(it)) } ?: OrdersBankStrings.backupNever
