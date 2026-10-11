package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/** The papers to scan for this move and the portal's one-PDF size rule; labels and the "when needed" lines are bundle content. */
internal fun LazyListScope.preClaimDocumentItems(ws: PreClaimWorksheet) {
    val sheet = ws.sheet ?: return
    item(key = "documents") {
        PreClaimSectionFrame(PreClaimStrings.documentsTitle, PreClaimStrings.uploadRule(ws.upload.files, ws.upload.format, ws.upload.moreThanKb, ws.upload.lessThanMb)) {
            sheet.documents.forEach { document ->
                Text("${GuideStrings.bullet} ${document.label}", style = MaterialTheme.typography.bodyMedium)
                if (document.whenNeeded.isNotEmpty()) PreClaimQuietText(document.whenNeeded)
            }
        }
    }
}

/** The move's own notes from the bundle, then the notes the calculators raised (such as the 180-day limit). */
internal fun LazyListScope.preClaimNoteItems(ws: PreClaimWorksheet) {
    val lines = ws.sheet?.notes.orEmpty() + ws.notes.map(PreClaimStrings::note)
    if (lines.isEmpty()) return
    item(key = "notes") {
        PreClaimSectionFrame(PreClaimStrings.notesTitle) {
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
