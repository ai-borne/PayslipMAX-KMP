package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.runtime.Composable
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pdfparser.utils.PdfLetterFormatter
import com.payslipmax.pdfparser.utils.sharePdf
import com.payslipmax.pdfparser.utils.shareText

@Composable
internal fun PcdaoAuditDialogs(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)?,
) {
    if (uiState.isAddFactorSheetVisible) {
        AddFactorBottomSheet(
            activeFactors = uiState.activeContext.activeSpecializedFactors,
            onToggleFactor = { viewModel.toggleSpecializedFactor(it) },
            onDismiss = { viewModel.setAddFactorSheetVisible(false) },
        )
    }
    uiState.generatedLetter?.let { letter ->
        RedressalPreviewBottomSheet(
            letter = letter,
            onDismiss = { viewModel.clearGeneratedLetter() },
            onExportPdf = { draft ->
                sharePdf(
                    fileName = PdfLetterFormatter.fileName(draft.disputeMonth),
                    title = draft.subject,
                    bodyText = draft.fullBodyText,
                )
            },
            onShareText = { draft -> shareText(text = draft.fullBodyText, title = draft.subject) },
            onEditText = { draft ->
                viewModel.clearGeneratedLetter()
                onNavigateToRepresentation?.invoke(draft)
            },
            onToggleMaskPii = { isMasked -> viewModel.generateRedressalLetter(maskPii = isMasked) },
        )
    }
}
