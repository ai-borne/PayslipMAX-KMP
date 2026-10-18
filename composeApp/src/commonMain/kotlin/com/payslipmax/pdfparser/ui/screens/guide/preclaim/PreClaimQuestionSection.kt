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

/** The portal's basic-information questions for this move, labels and order straight from the bundle. Each is a Yes/No on the portal, so each is a tick. */
@Composable
private fun PreClaimQuestionSection(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    PreClaimSectionFrame(PreClaimStrings.questionsTitle) {
        ws.questions.forEach { question ->
            PreClaimTickRow(question.label, question.id in typed.ticks) { model.setTick(question.id, it) }
        }
    }
}
