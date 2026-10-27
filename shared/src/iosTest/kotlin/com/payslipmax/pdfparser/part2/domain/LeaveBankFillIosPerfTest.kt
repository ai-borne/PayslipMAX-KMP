package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves the Leave Bank fill is fast on Kotlin/Native's runtime, not just correct on the JVM (plan 12, Cross-cutting checks). The form
 * rebuilds its notes and the slip its check on every keystroke, and a press reads the whole statement and every other draft. Plain scans only,
 * no regex; this is the automated gate for that.
 */
class LeaveBankFillIosPerfTest {
    private val rows = (1966..2025).map { accrued("$it-12-31", 30, (it - 1965) * 5) } + (2000..2025).map { ltcEncashed("$it-04-26", 5) }
    private val leaves =
        (0 until 200).map { n ->
            val day = CalendarDate(2024, 1 + n % 12, 1 + n % 27)
            OccurrenceRequest("GTDAL", mapOf("leave_from" to SlotValue.DateValue(day), "leave_to" to SlotValue.DateValue(day.copy(day = day.day + 1)), "year" to SlotValue.NumberValue(2024)))
        }
    private val acclve = Part2Rules.shipped().fill("ACCLVE")!!
    private val encash = Part2Rules.shipped().fill("ENCASH")!!

    @Test
    fun aFullStatementAndTwoHundredDraftsFillTheYearEndFormTwoThousandTimesWithinTheBound() {
        val started = TimeSource.Monotonic.markNow()
        val inputs = LeaveBankInputs(rows, leaves, leaves)
        val request = OccurrenceRequest("ACCLVE", mapOf("year" to SlotValue.NumberValue(2024)))
        var filled = 0
        repeat(2_000) { filled += (acclve.fill(request, inputs) as FillOutcome.Values).values.size }
        val elapsed = started.elapsedNow().inWholeMilliseconds
        assertEquals(2_000 * 4, filled)
        assertTrue(elapsed < BOUND_MS, "2,000 fills took $elapsed ms")
    }

    @Test
    fun theBreakUpFillItsNotesAndTheCheckAreWorkedOutTenThousandTimesWithinTheBound() {
        val breakup = (encash.fill(OccurrenceRequest("ENCASH"), LeaveBankInputs(rows, emptyList(), emptyList())) as FillOutcome.Values).values.getValue("breakup")
        val typed = OccurrenceRequest("ENCASH", mapOf("breakup" to breakup))
        val figures = OccurrenceRequest("ACCLVE", mapOf("year" to SlotValue.NumberValue(2024), "prev_total" to SlotValue.NumberValue(100), "ltc_days" to SlotValue.NumberValue(5), "accrued" to SlotValue.NumberValue(30)))
        val started = TimeSource.Monotonic.markNow()
        var notes = 0
        repeat(10_000) {
            notes += encash.notes(typed, rows).size
            if (acclve.crossCheck(figures, rows) != null) notes++
        }
        val elapsed = started.elapsedNow().inWholeMilliseconds
        assertTrue(notes > 0)
        assertTrue(elapsed < BOUND_MS, "10,000 note and check passes took $elapsed ms")
    }

    @Test
    fun theInputsAreBuiltFromTwoHundredStoredDraftsFiveHundredTimesWithinTheBound() {
        val drafts = leaves.mapIndexed { n, request -> Part2Draft("d$n", LeaveEventInput(listOf(request)), emptyMap(), 1L, n.toLong(), PrintedSnapshot(5, emptyMap(), emptyList())) }
        val started = TimeSource.Monotonic.markNow()
        var seen = 0
        repeat(500) { seen += LeaveBankInputs.of(rows, drafts, emptyList(), "me").printed.size }
        val elapsed = started.elapsedNow().inWholeMilliseconds
        assertEquals(500 * 200, seen)
        assertTrue(elapsed < BOUND_MS, "500 builds over 200 drafts took $elapsed ms")
    }

    private companion object {
        const val BOUND_MS = 5_000L
    }
}
