package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListSpacing
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.ChoicePicker
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.DraftRowUi
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DeleteButton
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDateBox
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimTextBox
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/** What one order's screen can do: type its number, pick its date, scan more pages, show or delete a page, open its draft or delete it. */
class VaultOrderActions(
    val onNumber: (String) -> Unit,
    val onDate: (CalendarDate?) -> Unit,
    val onAddPages: () -> Unit,
    val loadPage: suspend (pageId: String) -> ImageBitmap?,
    val onDeletePage: (pageId: String) -> Unit,
    val onLink: (draftId: String?) -> Unit,
    val onOpenDraft: () -> Unit,
    val onDelete: () -> Unit,
    val onDismissNotice: () -> Unit,
)

/**
 * One published order: its number and date, its pages (shown one at a time, on request) and what can be done with it, including linking it
 * to one of the officer's [drafts] and unlinking it. Stateless. An
 * officer whose Premium ended ([canEdit] false) reads it and deletes it; they cannot type, scan or change anything.
 */
@Composable
internal fun VaultOrderScreen(
    state: VaultOrderUiState,
    notice: String?,
    canEdit: Boolean,
    drafts: List<DraftRowUi>,
    actions: VaultOrderActions,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2VaultStrings.orderScreenTitle, subtitle = Part2VaultStrings.orderScreenSubtitle, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Part2ListSpacing) {
            if (!canEdit) item(key = "view-only") { PreClaimQuietText(Part2VaultStrings.orderViewOnlyNote) }
            notice?.let { item(key = "notice") { VaultNoticeView(it, actions.onDismissNotice) } }
            item(key = "fields") { OrderFields(state, canEdit, actions) }
            item(key = "pages-heading") { Text(Part2VaultStrings.pagesHeading, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() }) }
            items(state.pages, key = { it.id }) { VaultPageRow(it, state.pages.size == 1, actions.loadPage) { actions.onDeletePage(it.id) } }
            item(key = "actions") { OrderActions(state, canEdit, drafts, actions) }
        }
    }
}

/** The number and date: two boxes for an entitled officer, two lines of text for one who can only read. */
@Composable
private fun OrderFields(
    state: VaultOrderUiState,
    canEdit: Boolean,
    actions: VaultOrderActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (canEdit) {
            PreClaimTextBox(Part2Strings.orderNoLabel, state.orderNo, actions.onNumber, numeric = false, supporting = state.numberError, error = state.numberError != null)
            PreClaimDateBox(Part2Strings.orderDateLabel, state.orderDate, actions.onDate)
            if (state.saveFailed) Text(Part2VaultStrings.saveFailed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        } else {
            Text(if (state.orderNo.isEmpty()) Part2VaultStrings.numberNotTyped else "${Part2Strings.orderNoLabel}: ${state.orderNo}", style = MaterialTheme.typography.bodyLarge)
            state.orderDate?.let { Text("${Part2Strings.orderDateLabel}: ${CivilCalendar.format(it)}", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun OrderActions(
    state: VaultOrderUiState,
    canEdit: Boolean,
    drafts: List<DraftRowUi>,
    actions: VaultOrderActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (canEdit && state.canAddPages) Button(onClick = actions.onAddPages, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(Part2VaultStrings.addPages) }
        if (state.draftId != null) OutlinedButton(onClick = actions.onOpenDraft, modifier = Modifier.fillMaxWidth()) { Text(Part2VaultStrings.openDraft) }
        if (canEdit && state.linked) OutlinedButton(onClick = { actions.onLink(null) }, modifier = Modifier.fillMaxWidth()) { Text(Part2VaultStrings.unlinkDraft) }
        if (canEdit && !state.linked) ChoicePicker(Part2VaultStrings.linkToDraft, drafts, { Part2VaultStrings.linkChoice(it.title, it.line) }) { actions.onLink(it.id) }
        Part2DeleteButton(Part2VaultStrings.deleteOrder, Part2VaultStrings.deleteOrderTitle, Part2VaultStrings.deleteOrderBody, actions.onDelete)
    }
}
