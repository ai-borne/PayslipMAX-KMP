package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.GuideTileFrame
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListSpacing
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Message
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings

/** The Due list: what is due now, then what comes next. Stateless; a row opens its draft's slip. [onOpenSettings] is non-null exactly when the "reminders are off" note is shown. */
@Composable
internal fun DueScreen(
    state: DueUiState,
    onOpen: (draftId: String) -> Unit,
    onDismiss: (DueRowUi) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
    onOpenSettings: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2DueStrings.screenTitle, subtitle = Part2DueStrings.screenSubtitle, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Part2ListSpacing) {
            if (onOpenSettings != null) item(key = "reminders-off") { RemindersOffNote(onOpenSettings) }
            if (state.unreadable) item(key = "unreadable") { Part2Message(Part2DueStrings.unreadable) }
            if (state.isEmpty) item(key = "empty") { Part2Message(Part2DueStrings.empty) }
            dueSection(Part2DueStrings.dueHeading, state.due, onOpen, onDismiss)
            dueSection(Part2DueStrings.upcomingHeading, state.upcoming, onOpen, onDismiss)
        }
    }
}

/** The way back after a declined notification permission (Round 44): one line and a button to the system settings. */
@Composable
private fun RemindersOffNote(onOpenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(Part2ReminderStrings.settingsNote, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onOpenSettings) { Text(Part2ReminderStrings.settingsOpen) }
    }
}

private fun LazyListScope.dueSection(
    heading: String,
    rows: List<DueRowUi>,
    onOpen: (String) -> Unit,
    onDismiss: (DueRowUi) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "heading-$heading") { Text(heading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
    items(rows, key = { "$heading-${it.key}" }) { DueRowView(it, onClick = { onOpen(it.draftId) }, onDismiss = { onDismiss(it) }) }
}

@Composable
private fun DueRowView(
    row: DueRowUi,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        GuideTileFrame(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
                Text(row.action, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(row.title, style = MaterialTheme.typography.bodyMedium)
                Text(row.line, style = MaterialTheme.typography.bodyMedium, color = if (row.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onDismiss) { Text(Part2DueStrings.dismiss) }
    }
}
