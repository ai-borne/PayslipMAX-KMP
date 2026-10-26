package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.SlotValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A year-by-year list is stored as an ordinary text answer (phase 8), so the size bound on text must let a full list in and still keep a
 * crafted file from carrying a huge one. The numbers are pinned by literal so a change is a decision (Part2LimitsPinTest's reason).
 */
class Part2BreakupBoundsTest {
    private fun draftWith(text: String) = Part2Draft("d1", LeaveEventInput(listOf(OccurrenceRequest("X", mapOf("breakup" to SlotValue.TextValue(text))))), emptyMap(), 1L, 1L)

    @Test
    fun theBreakupLimitsAreTheOnesTheOwnerCanSeeAndALongestListFitsExactly() {
        assertEquals(600, Part2Limits.BREAKUP_TEXT_MAX)
        assertEquals(60, Part2Limits.BREAKUP_ROWS_MAX)
        val longest = (0 until Part2Limits.BREAKUP_ROWS_MAX).joinToString("\n") { "${1960 + it} 9999" }
        assertEquals(Part2Limits.BREAKUP_TEXT_MAX - 1, longest.length, "60 rows of YYYY DDDD and 59 newlines")
    }

    @Test
    fun aListUpToTheBreakupLimitIsStoredAndOneCharacterMoreIsRefused() {
        assertNull(Part2Bounds.check(draftWith("x".repeat(Part2Limits.BREAKUP_TEXT_MAX))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draftWith("x".repeat(Part2Limits.BREAKUP_TEXT_MAX + 1))))
    }
}
