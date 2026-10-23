package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class Part2DraftSummaryTest {
    private val engine = Part2DraftEngine(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())

    private fun day(
        m: Int,
        d: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, m, d))

    private fun draft(vararg requests: OccurrenceRequest) = Part2Draft("id-1", LeaveEventInput(requests.toList()), emptyMap(), 1L, 77L)

    private val complete =
        OccurrenceRequest(
            GRANT,
            mapOf("leave_from" to day(7, 5), "leave_to" to day(9, 4), "year" to SlotValue.NumberValue(2026), "away_from" to day(7, 1)),
            setOf(ANSWER_RATION),
        )

    @Test
    fun aCompleteDraftShowsItsRequestedSpanNotTheLinkedOrdersDates() {
        val summary = Part2DraftSummary.of(draft(complete), engine)
        assertEquals(CalendarDate(2026, 7, 5), summary.from, "the ration order starts on 01/07, earlier, but it was not asked for")
        assertEquals(CalendarDate(2026, 9, 4), summary.to)
        assertEquals(listOf(GRANT), summary.codes)
        assertEquals("id-1", summary.id)
        assertEquals(77L, summary.updatedAt)
    }

    @Test
    fun anUnfinishedDraftHasCodesButNoSpan() {
        val summary = Part2DraftSummary.of(draft(OccurrenceRequest(GRANT, mapOf("leave_from" to day(7, 5)))), engine)
        assertEquals(listOf(GRANT), summary.codes)
        assertNull(summary.from)
        assertNull(summary.to)
    }

    @Test
    fun theSpanCoversEveryRequestedOccurrenceAndEachCodeIsListedOnce() {
        val second = complete.copy(values = complete.values + ("leave_from" to day(11, 2)) + ("leave_to" to day(11, 20)))
        val summary = Part2DraftSummary.of(draft(complete, second), engine)
        assertEquals(CalendarDate(2026, 7, 5), summary.from)
        assertEquals(CalendarDate(2026, 11, 20), summary.to)
        assertEquals(listOf(GRANT), summary.codes)
    }

    @Test
    fun theTextFormHidesEverything() {
        assertFalse("2026" in Part2DraftSummary.of(draft(complete), engine).toString())
        assertFalse(GRANT in Part2DraftSummary.of(draft(complete), engine).toString())
    }
}
