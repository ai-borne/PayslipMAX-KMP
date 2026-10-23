package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListSpacing
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/**
 * The slip the clerk is handed, as the officer checks it: the orders and rows, then the checks against the manual, then what could not be
 * drafted. A user whose Premium ended ([canEdit] false) reads and deletes it and cannot change it. Until the answers are complete the
 * slip is not drawn: the screen says what is still needed instead. Printing is Phase 5; nothing here prints.
 */
@Composable
internal fun SlipPreviewScreen(
    state: LeaveDraftUiState,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onTick: (key: String, ticked: Boolean) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    val slip = state.slip
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2Strings.slipTitle, subtitle = Part2Strings.slipSubtitle, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Part2ListSpacing) {
            if (!canEdit) item(key = "view-only") { PreClaimQuietText(Part2Strings.viewOnlyNote) }
            if (slip == null) {
                item(key = "unfinished") { PreClaimQuietText(Part2Strings.unfinishedLine(state.missing)) }
            } else {
                item(key = "last-order") { Text(slip.lastOrder, style = MaterialTheme.typography.bodyMedium) }
                items(slip.orders, key = { it.title }) { SlipOrderView(it) }
                if (slip.findings.isNotEmpty()) item(key = "findings") { FindingsView(slip, onTick) }
                if (slip.undrafted.isNotEmpty()) item(key = "undrafted") { UndraftedView(slip.undrafted) }
            }
            item(key = "actions") { SlipActions(canEdit, onEdit, onDelete) }
        }
    }
}

@Composable
private fun SlipActions(
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (canEdit) Button(onClick = onEdit) { Text(Part2Strings.editDraft) }
        Part2DeleteButton(onDelete)
    }
}
