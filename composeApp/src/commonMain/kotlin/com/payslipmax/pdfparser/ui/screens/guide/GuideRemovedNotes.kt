package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideColors
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings

/** The notes lines Guide Home needs: how many rows could not be read, how many sit on removed cards, and how to open that list. */
internal class GuideHomeNotes(
    val unreadable: Int = 0,
    val removedCount: Int = 0,
    val onOpenRemoved: () -> Unit = {},
) {
    companion object {
        val None = GuideHomeNotes()
    }
}

/** The quiet line on Guide Home when some stored notes could not be read. No button: the rows stay on the device. */
@Composable
internal fun GuideUnreadableNotesLine(count: Int) {
    Text(
        GuideMaintenanceStrings.notesUnreadable(count),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingSmall),
    )
}

/** The Guide Home row that opens the list of notes whose card is gone; shown only when there are some. */
@Composable
internal fun GuideRemovedNotesRow(
    count: Int,
    onClick: () -> Unit,
) {
    GuideTileFrame(onClick = onClick) {
        Text(
            GuideMaintenanceStrings.notesRemovedRow(count),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(AppDimensions.SpacingMedium),
        )
    }
}

/**
 * Notes whose card no longer exists in the Guide, newest first, each with its card id and a Delete button (after a
 * confirmation). The ids and words are the user's own and stay on screen only; nothing here is shared or logged.
 */
@Composable
internal fun GuideRemovedNotesScreen(
    notes: List<GuideRemovedNote>,
    onBack: () -> Unit,
    onDelete: (cardId: String, onDone: (Boolean) -> Unit) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    var deleting by remember { mutableStateOf<GuideRemovedNote?>(null) }
    var failed by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = GuideMaintenanceStrings.notesRemovedTitle, subtitle = GuideMaintenanceStrings.notesRemovedSubtitle, onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            if (failed) item(key = "failed") { Text(GuideMaintenanceStrings.noteFailed, style = MaterialTheme.typography.bodySmall, color = GuideColors.watchOut()) }
            if (notes.isEmpty()) item(key = "empty") { Text(GuideMaintenanceStrings.notesRemovedNone, style = MaterialTheme.typography.bodyMedium) }
            items(notes, key = { it.cardId }) { note -> RemovedNoteCard(note, onDelete = { deleting = note }) }
        }
    }
    deleting?.let { note ->
        GuideNoteDeleteDialog(onCancel = { deleting = null }, onConfirm = {
            onDelete(note.cardId) { done -> failed = !done }
            deleting = null
        })
    }
}

@Composable
private fun RemovedNoteCard(
    note: GuideRemovedNote,
    onDelete: () -> Unit,
) {
    FlatBorderedCardShape(tint = CardTint.Accent) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(note.cardId, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(note.text, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onDelete) { Text(GuideMaintenanceStrings.noteDelete) }
        }
    }
}
