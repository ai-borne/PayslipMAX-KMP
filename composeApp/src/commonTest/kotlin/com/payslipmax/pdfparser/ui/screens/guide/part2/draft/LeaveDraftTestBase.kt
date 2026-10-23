package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/** Delegates to a real fake, counting saves and optionally refusing them, so autosave can be timed and its failures shown. */
internal class Recording(
    private val inner: Part2Repository = FakePart2Repository(),
) : Part2Repository by inner {
    var saves = 0
    var refuse: Part2WriteResult? = null

    override suspend fun saveDraft(draft: Part2Draft): Part2WriteResult {
        saves++
        return refuse ?: inner.saveDraft(draft)
    }
}

/** What the draft ViewModel tests share: a recording repository, a clock they move, and a model over the invented templates. */
abstract class LeaveDraftTestBase {
    protected var clock = 1_000L
    internal val repo = Recording()
    protected val from = CalendarDate(2026, 7, 5)

    protected fun TestScope.model(
        start: LeaveEventInput? = LeaveEventEditor.start(GRANT),
        id: String = "draft-1",
    ) = LeaveDraftViewModel(id, SyntheticPart2Templates.lookup, repo, start, SyntheticPart2Templates.registry(), { clock }, StandardTestDispatcher(testScheduler))

    protected suspend fun saved(id: String = "draft-1") = repo.draft(id)

    protected fun LeaveDraftViewModel.field(slot: String) = state.value.sections[0].fields.first { it.slotId == slot }

    companion object {
        fun complete(
            vararg answers: String,
            extra: Map<String, SlotValue> = emptyMap(),
        ) = LeaveEventInput(
            listOf(
                OccurrenceRequest(
                    GRANT,
                    mapOf("leave_from" to SlotValue.DateValue(CalendarDate(2026, 7, 5)), "leave_to" to SlotValue.DateValue(CalendarDate(2026, 9, 4)), "year" to SlotValue.NumberValue(2026)) + extra,
                    answers.toSet(),
                ),
            ),
        )
    }
}
