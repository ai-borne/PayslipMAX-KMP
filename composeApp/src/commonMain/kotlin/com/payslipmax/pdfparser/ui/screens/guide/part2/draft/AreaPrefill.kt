package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.SlotValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The inputs a choice newly asks for (the area details of R&H, HAA, SCCIA and the field areas) are filled from the newest other draft that made the same
 * choice (plan 12: prefilled from the last draft of the same code, no profile store). Only those inputs, only where empty: never
 * a date, never an answer, never what the officer already typed. Drafts arrive newest edit first. [event] reads the officer's current
 * answers, [fieldIds] the inputs the form asks for a request now, and [edit] makes one atomic change to the answers.
 */
internal class AreaPrefill(
    private val scope: CoroutineScope,
    private val repository: Part2Repository,
    private val draftId: String,
    private val event: () -> LeaveEventInput,
    private val fieldIds: (OccurrenceRequest) -> List<String>,
    private val edit: ((LeaveEventInput) -> LeaveEventInput) -> Unit,
) {
    /** A choice was made on request [index] ([answer] is held now); [before] is what the form asked for that request before the choice. */
    fun after(
        index: Int,
        answer: String,
        before: List<String>,
    ) {
        val asked = event().requests.getOrNull(index)?.let(fieldIds).orEmpty() - before.toSet()
        if (asked.isEmpty()) return
        scope.launch {
            val source = repository.observeDrafts().first().items.filter { it.id != draftId }.firstNotNullOfOrNull { draft -> draft.event.requests.firstOrNull { answer in it.answers } } ?: return@launch
            for (slot in asked) {
                val value = source.values[slot]?.takeUnless { it is SlotValue.DateValue } ?: continue
                edit { current ->
                    val request = current.requests.getOrNull(index)
                    if (request == null || answer !in request.answers || slot in request.values) current else LeaveEventEditor.setValue(current, index, slot, value)
                }
            }
        }
    }
}
