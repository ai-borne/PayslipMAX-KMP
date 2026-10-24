package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: after printing, the preview must tell the officer whether the paper in the clerk's hand is still the draft: it says when it was
 * printed, and says in words (never built in a composable) whether the draft has changed since or the manual's template has moved.
 */
class LeaveDraftPrintedStateTest {
    private val builder = LeaveDraftStateBuilder(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())

    private fun event(year: Int = 2026) =
        LeaveEventInput(
            listOf(
                OccurrenceRequest(
                    GRANT,
                    mapOf(
                        "leave_from" to SlotValue.DateValue(CalendarDate(2026, 7, 5)),
                        "leave_to" to SlotValue.DateValue(CalendarDate(2026, 9, 4)),
                        "year" to SlotValue.NumberValue(year),
                    ),
                ),
            ),
        )

    private val printedAt = 86_400_000L * 40

    private fun state(
        event: LeaveEventInput,
        snapshotOf: LeaveEventInput = event(),
        revs: Map<String, String>? = null,
        printing: Boolean = false,
    ): LeaveDraftUiState {
        val snapshot = builder.snapshotOf(snapshotOf, printedAt)!!.let { if (revs == null) it else it.copy(revs = revs) }
        return builder.build(event, emptySet(), false, snapshot, printing)
    }

    @Test
    fun aDraftNeverPrintedShowsNoPrintLine() {
        val state = builder.build(event(), emptySet(), false)

        assertNull(state.printed)
        assertEquals(false, state.printing)
    }

    @Test
    fun aDraftThatStillMatchesItsPrintShowsWhenItWasPrintedAndNothingElse() {
        val printed = state(event()).printed!!

        assertEquals(Part2Strings.printedOnLine("10/02/1970"), printed.line)
        assertNull(printed.note)
    }

    @Test
    fun aDraftEditedSincePrintingSaysChanged() {
        assertEquals(Part2Strings.changedSincePrinted, state(event(year = 2027)).printed!!.note)
    }

    @Test
    fun aDraftThatIsUnfinishedNowSaysChangedToo() {
        val unfinished = LeaveEventInput(listOf(OccurrenceRequest(GRANT)))

        val state = state(unfinished)

        assertNull(state.slip)
        assertEquals(Part2Strings.changedSincePrinted, state.printed!!.note)
    }

    @Test
    fun aTemplateRevisionThatMovedSaysTheTemplateWasUpdated() {
        assertEquals(Part2Strings.templateUpdatedSincePrinted, state(event(), revs = mapOf(GRANT to "a0000000")).printed!!.note)
    }

    @Test
    fun thePrintingFlagIsPassedThrough() {
        assertTrue(state(event(), printing = true).printing)
    }

    @Test
    fun theSnapshotOfACompleteEventHoldsItsRevisionsRowsAndTime() {
        val snapshot = assertNotNull(builder.snapshotOf(event(), 77L))

        assertEquals(77L, snapshot.at)
        assertEquals(mapOf(GRANT to "a0000001"), snapshot.revs)
        assertEquals(listOf("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026"), snapshot.rows.map { it.narrative })
    }

    @Test
    fun anIncompleteEventHasNothingToSnapshot() {
        assertNull(builder.snapshotOf(LeaveEventInput(listOf(OccurrenceRequest(GRANT))), 77L))
    }
}
