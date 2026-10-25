package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the drafts list says a draft is "Not finished" when it has no dates yet. A cancel slip has no dates at all and is complete once it
 * quotes the order, so the list must be able to tell the two apart (found on the Pixel 9 walk, 2026-10-25). Run on the shipped templates.
 */
class Part2DraftSummaryRealBundleTest {
    private fun summary(request: OccurrenceRequest) = Part2DraftSummary.of(Part2Draft("d", LeaveEventInput(listOf(request)), emptyMap(), 1L, 2L), RealPart2.engine)

    @Test
    fun aCompleteCancelSlipHasNoSpanAndIsFinished() {
        val cancel = summary(OccurrenceRequest("CANCEL", mapOf("cancelled" to ref("A/1", 6, 20))))

        assertNull(cancel.from)
        assertNull(cancel.to)
        assertTrue(cancel.finished)
    }

    @Test
    fun aLeaveDraftStillBeingFilledInIsNotFinishedAndACompleteOneIs() {
        val partial = summary(OccurrenceRequest("GTDAL", mapOf("leave_from" to date(7, 6))))
        val complete = summary(OccurrenceRequest("GTDAL", mapOf("leave_from" to date(7, 6), "leave_to" to date(8, 4), "year" to number(2026))))

        assertFalse(partial.finished)
        assertTrue(complete.finished)
        assertEquals(date(7, 6).date, complete.from)
    }

    @Test
    fun aCancelsSlipMissingItsCodeAndSerialIsNotFinished() {
        assertFalse(summary(OccurrenceRequest("CANCELS", mapOf("cancelled" to ref("A/1", 6, 20)))).finished)
    }
}
