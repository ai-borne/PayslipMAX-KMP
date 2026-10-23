package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.guide.GuideTileFrame
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Message
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** The "Your drafts" rows on the Part II home: nothing while the device holds none. Each opens on its slip and has its own delete. */
internal fun LazyListScope.part2DraftItems(drafts: Part2DraftList) {
    val state = drafts.state
    if (!state.hasAny && !state.unreadable) return
    item(key = "drafts-heading") { Text(Part2Strings.draftsHeading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
    if (state.unreadable) item(key = "drafts-unreadable") { Part2Message(Part2Strings.draftsUnreadable) }
    items(state.rows, key = { "draft-${it.id}" }) { row ->
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            GuideTileFrame(onClick = { drafts.onOpen(row.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
                    Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(row.line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Part2DeleteButton { drafts.onDelete(row.id) }
        }
    }
}

/** "Delete draft", which asks first: a draft is the officer's work and there is no undo. [onDelete] runs only after "Delete". */
@Composable
internal fun Part2DeleteButton(onDelete: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    TextButton(onClick = { asking = true }) { Text(Part2Strings.deleteDraft) }
    if (!asking) return
    AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text(Part2Strings.deleteTitle) },
        text = { Text(Part2Strings.deleteBody) },
        confirmButton = {
            TextButton(onClick = {
                asking = false
                onDelete()
            }) { Text(Part2Strings.deleteConfirm) }
        },
        dismissButton = { TextButton(onClick = { asking = false }) { Text(Part2Strings.deleteCancel) } },
    )
}
