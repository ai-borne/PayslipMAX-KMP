package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

internal fun LazyListScope.preClaimQuestionItems(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    if (ws.questions.isEmpty()) return
    item(key = "questions") { PreClaimQuestionSection(ws, typed, model) }
}

/** The portal's basic-information questions for this move, labels and order straight from the bundle. Marital status is typed text. */
@Composable
private fun PreClaimQuestionSection(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    PreClaimSectionFrame(PreClaimStrings.questionsTitle) {
        ws.questions.forEach { question ->
            if (question.id == PreClaimFieldIds.MARITAL_STATUS) {
                PreClaimTextBox(question.label, typed.maritalStatus, model::setMaritalStatus, numeric = false, supporting = question.hint.ifEmpty { null })
            } else {
                PreClaimTickRow(question.label, question.id in typed.ticks) { model.setTick(question.id, it) }
            }
        }
    }
}
