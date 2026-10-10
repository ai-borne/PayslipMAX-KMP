package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideColors
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings

/**
 * What a card screen needs to show and change the user's notes on [cardId]. It exists only for an unlocked card, so a locked
 * card has no note controls at all (see [rememberGuideNoteControls]). [onSave] gets the typed text and, for a carried note
 * being edited on this card, the earlier rule's id to move it from.
 */
internal class GuideNoteControls(
    val cardId: String,
    val notes: GuideCardNotes,
    val onSave: (text: String, movedFrom: String?, onDone: (GuideNoteOutcome) -> Unit) -> Unit,
    val onDelete: (sourceCardId: String, onDone: (Boolean) -> Unit) -> Unit,
)

/**
 * The note controls for [card], or null while the card is locked ([GuideCardContent.full] is null) or notes are not wired.
 * The check comes before the notes are collected, so a locked card never reads a note.
 */
@Composable
internal fun rememberGuideNoteControls(
    card: GuideCardContent,
    notes: GuideNotesViewModel?,
): GuideNoteControls? {
    if (notes == null || card.full == null) return null
    val state by notes.state.collectAsState()
    return GuideNoteControls(
        cardId = card.id,
        notes = state.forCard(card.id),
        onSave = { text, movedFrom, onDone -> notes.save(card.id, text, movedFrom, onDone) },
        onDelete = notes::delete,
    )
}

/** The notes state a screen draws from, or none while notes are not wired. Collected only while that screen is shown. */
@Composable
internal fun rememberGuideNotesState(notes: GuideNotesViewModel?): GuideNotesState {
    if (notes == null) return GuideNotesState.None
    val state by notes.state.collectAsState()
    return state
}

/** The cards that show a "Note" marker: the user's own notes, and only while the Guide is unlocked for them. */
@Composable
internal fun rememberNotedCards(
    notes: GuideNotesViewModel?,
    access: GuideAccess,
): Set<String> {
    if (!access.isUnlocked) return emptySet()
    return rememberGuideNotesState(notes).notedCards
}

/** The "Your note" block, after the card's body and before the closing disclaimer. */
internal fun LazyListScope.guideNoteItems(controls: GuideNoteControls) {
    item(key = "note") { GuideYourNote(controls) }
}

private sealed interface NoteDialog {
    data object Add : NoteDialog

    class Edit(val item: GuideNoteItem) : NoteDialog

    class Delete(val item: GuideNoteItem) : NoteDialog
}

@Composable
private fun GuideYourNote(controls: GuideNoteControls) {
    var dialog by remember { mutableStateOf<NoteDialog?>(null) }
    var failed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
        controls.notes.own?.let { own ->
            GuideNoteCard(own, GuideMaintenanceStrings.noteStale, onEdit = { dialog = NoteDialog.Edit(own) }, onDelete = { dialog = NoteDialog.Delete(own) })
        }
        if (controls.notes.own == null) GuideAddNote(onAdd = { dialog = NoteDialog.Add })
        if (controls.notes.carried.isNotEmpty()) GuideCarriedNotes(controls.notes.carried, onEdit = { dialog = NoteDialog.Edit(it) }, onDelete = { dialog = NoteDialog.Delete(it) })
        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = GuideColors.watchOut()) }
    }
    GuideNoteDialogs(dialog, failed, controls, close = {
        dialog = null
        failed = false
    }, onFailed = { failed = true }, onNotice = { notice = it })
}

@Composable
private fun GuideNoteDialogs(
    dialog: NoteDialog?,
    failed: Boolean,
    controls: GuideNoteControls,
    close: () -> Unit,
    onFailed: () -> Unit,
    onNotice: (String?) -> Unit,
) {
    when (dialog) {
        null -> Unit
        NoteDialog.Add -> GuideNoteEditorDialog("", failed, onCancel = close, onSave = { text -> controls.save(text, null, close, onFailed, onNotice) })
        is NoteDialog.Edit ->
            GuideNoteEditorDialog(dialog.item.text, failed, onCancel = close, onSave = { text ->
                controls.save(text, dialog.item.sourceCardId.takeIf { it != controls.cardId }, close, onFailed, onNotice)
            })
        is NoteDialog.Delete ->
            GuideNoteDeleteDialog(onCancel = close, onConfirm = {
                controls.onDelete(dialog.item.sourceCardId) { done ->
                    onNotice(if (done) null else GuideMaintenanceStrings.noteFailed)
                    close()
                }
            })
    }
}

private fun GuideNoteControls.save(
    text: String,
    movedFrom: String?,
    close: () -> Unit,
    onFailed: () -> Unit,
    onNotice: (String?) -> Unit,
) = onSave(text, movedFrom) { outcome ->
    when (outcome) {
        GuideNoteOutcome.FAILED -> onFailed()
        GuideNoteOutcome.SAVED_NOT_MOVED -> {
            onNotice(GuideMaintenanceStrings.noteMoveIncomplete)
            close()
        }
        GuideNoteOutcome.SAVED, GuideNoteOutcome.DELETED -> {
            onNotice(null)
            close()
        }
    }
}

@Composable
private fun GuideAddNote(onAdd: () -> Unit) {
    FlatBorderedCardShape(tint = CardTint.Accent) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            NoteHeading(GuideMaintenanceStrings.noteSectionTitle)
            Text(GuideMaintenanceStrings.noteNotOfficial, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAdd) { Text(GuideMaintenanceStrings.noteAdd) }
        }
    }
}

@Composable
private fun GuideCarriedNotes(
    items: List<GuideNoteItem>,
    onEdit: (GuideNoteItem) -> Unit,
    onDelete: (GuideNoteItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        NoteHeading(GuideMaintenanceStrings.noteCarriedHeading)
        items.forEach { item ->
            GuideNoteCard(item, GuideMaintenanceStrings.noteCarriedStale, onEdit = { onEdit(item) }.takeIf { item.editable }, onDelete = { onDelete(item) })
        }
    }
}

/** One note: heading, the stale line when it applies, the text, and its buttons. [onEdit] is null when editing is not allowed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideNoteCard(
    item: GuideNoteItem,
    staleText: String,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    FlatBorderedCardShape(tint = CardTint.Accent) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            NoteHeading(GuideMaintenanceStrings.noteSectionTitle)
            Text(GuideMaintenanceStrings.noteNotOfficial, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.stale) Text(staleText, style = MaterialTheme.typography.bodySmall, color = GuideColors.watchOut())
            Text(item.text, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                onEdit?.let { OutlinedButton(onClick = it) { Text(GuideMaintenanceStrings.noteEdit) } }
                TextButton(onClick = onDelete) { Text(GuideMaintenanceStrings.noteDelete) }
            }
        }
    }
}

@Composable
private fun NoteHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
}
