package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.domain.FillNote
import com.payslipmax.pdfparser.part2.domain.FillOutcome
import com.payslipmax.pdfparser.part2.domain.LeaveBankFill
import com.payslipmax.pdfparser.part2.domain.LeaveBankInputs
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.YearCheck
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import com.payslipmax.pdfparser.testing.FakeLeaveBankSource
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * What the Leave Bank fill tests share: an invented capability for the invented code (so the model's mechanics are proved apart from the real
 * ACCLVE and ENCASH rules), a statement, rules that carry the capability and a model over a source the test controls. Synthetic figures.
 */
internal abstract class LeaveDraftFillBase : LeaveDraftTestBase() {
    /** Fills `year` from the day the leave ends (so it reads the officer's CURRENT answers) and `leave_from` with a fixed day; records what it was handed. */
    internal class FakeFill : LeaveBankFill {
        override val code = GRANT
        override val slots = setOf("year", "leave_from")
        val asked = mutableListOf<LeaveBankInputs>()
        var outcome: (OccurrenceRequest) -> FillOutcome = { request ->
            val ends = (request.values["leave_to"] as? SlotValue.DateValue)?.date?.year ?: 2024
            FillOutcome.Values(mapOf("year" to SlotValue.NumberValue(ends), "leave_from" to SlotValue.DateValue(CalendarDate(2024, 1, 2))))
        }
        var check: ((OccurrenceRequest, List<LeaveEntry>) -> YearCheck?) = { _, rows -> YearCheck(2024, CrossCheck.Matches(rows.size)) }

        override fun fill(
            request: OccurrenceRequest,
            inputs: LeaveBankInputs,
        ): FillOutcome {
            asked += inputs
            return outcome(request)
        }

        override fun notes(
            request: OccurrenceRequest,
            rows: List<LeaveEntry>,
        ) = listOf<FillNote>(FillNote.StatementTotal(rows.size, 0))

        override fun crossCheck(
            request: OccurrenceRequest,
            rows: List<LeaveEntry>,
        ) = check(request, rows)
    }

    protected val rows = listOf(accrued("2023-12-31", 30, 282))
    protected val fill = FakeFill()

    protected val rules: Part2Rules =
        SyntheticPart2Templates.registry().let { base ->
            Part2Rules(base.ids.map { base.check(it) ?: checkNotNull(base.condition(it)) }, base.questions, listOf(fill))
        }

    protected fun TestScope.fillModel(
        source: LeaveBankSource? = FakeLeaveBankSource(rows),
        start: com.payslipmax.pdfparser.part2.domain.LeaveEventInput = LeaveEventEditor.start(GRANT),
    ) = LeaveDraftViewModel("draft-1", SyntheticPart2Templates.lookup, repo, start, rules, { clock }, StandardTestDispatcher(testScheduler), source) { 0L }

    protected fun LeaveDraftViewModel.value(slot: String) = state.value.sections[0].fields.first { it.slotId == slot }.value

    protected fun LeaveDraftViewModel.message() = state.value.sections[0].fill?.message
}
