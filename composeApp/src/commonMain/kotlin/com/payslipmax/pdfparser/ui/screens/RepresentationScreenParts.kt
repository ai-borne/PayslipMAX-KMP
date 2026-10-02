package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.utils.PdfLetterFormatter
import com.payslipmax.pdfparser.utils.sharePdf

/**
 * Export to PDF is gated by CLAIM_GENERATOR (Phase 4d): unlocked users get the share sheet; locked users
 * get the upgrade sheet via [onLocked] (D4). Everything else (edit/copy/text-share) stays free within this
 * already-premium screen.
 */
@Composable
internal fun rememberGatedPdfExport(
    viewModel: PayslipViewModel,
    onLocked: () -> Unit,
): (RepresentationDraftEntity) -> Unit {
    val hasClaimGenerator = viewModel.rememberHasAccess(FeatureGate.CLAIM_GENERATOR)
    return { draft ->
        if (hasClaimGenerator) {
            sharePdf(PdfLetterFormatter.fileName(draft.disputeMonth), draft.subject, draft.bodyText)
        } else {
            onLocked()
        }
    }
}

/** The open draft's editor, or the list of drafts when none is open. */
@Composable
internal fun RepresentationContent(
    drafts: List<RepresentationDraftEntity>,
    selectedDraft: RepresentationDraftEntity?,
    editedBody: String,
    onBodyChange: (String) -> Unit,
    onSave: (RepresentationDraftEntity) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onSelect: (RepresentationDraftEntity) -> Unit,
    onExportPdf: (RepresentationDraftEntity) -> Unit,
) {
    if (selectedDraft != null) {
        RepresentationEditor(
            draft = selectedDraft,
            editedBody = editedBody,
            onBodyChange = onBodyChange,
            onSave = { onSave(selectedDraft) },
            onCancel = onCancel,
        )
    } else {
        RepresentationDraftList(drafts = drafts, onBack = onBack, onSelect = onSelect, onExportPdf = onExportPdf)
    }
}
