package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_HILL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * The leave form redrafts the slip and re-derives its fields on every change (a key press, a date picked), on the main thread. This
 * proves that loop is fast on Kotlin/Native's runtime and not only correct on the JVM: an edit, the form's fields, the engine's draft
 * and the draft's summary, 2,000 times over a request with both linked orders, well inside a human pause. Plain scans and maps only,
 * no regex, as `Part2EngineIosPerfTest` pins for the engine alone.
 */
class Part2FormIosPerfTest {
    private val rules = SyntheticPart2Templates.registry()
    private val engine = Part2DraftEngine(SyntheticPart2Templates.lookup, rules)
    private val planner = LeaveFormPlanner(engine, rules)

    @Test
    fun twoThousandEditsEachRedraftAndRederiveTheFormWithinTheBound() {
        var event = LeaveEventEditor.start(GRANT)
        event = LeaveEventEditor.setYesNo(event, 0, ANSWER_RATION, true)
        event = LeaveEventEditor.setYesNo(event, 0, ANSWER_HILL, true)
        for ((slot, month, day) in listOf(Triple("away_from", 7, 1), Triple("rejoin", 9, 5), Triple("leave_to", 9, 4))) {
            event = LeaveEventEditor.setValue(event, 0, slot, SlotValue.DateValue(CalendarDate(2026, month, day)))
        }
        event = LeaveEventEditor.setValue(event, 0, "year", SlotValue.NumberValue(2026))
        val mark = TimeSource.Monotonic.markNow()
        var fields = 0
        repeat(EDITS) { i ->
            event = LeaveEventEditor.setValue(event, 0, "leave_from", SlotValue.DateValue(CalendarDate(2026, 7, 1 + i % 28)))
            fields += planner.fields(event.requests[0]).size
            val summary = Part2DraftSummary.of(Part2Draft("id", event, emptyMap(), 0L, 0L), engine)
            assertTrue(summary.codes == listOf(GRANT))
            assertIs<DraftResult.Drafted>(engine.draft(event))
        }
        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
        assertEquals(EDITS * 5, fields, "the form asks for five fields with both linked orders on")
        assertTrue(elapsedMs < BOUND_MS, "$EDITS redrafts took ${elapsedMs}ms, expected < ${BOUND_MS}ms")
    }

    private companion object {
        const val EDITS = 2_000
        const val BOUND_MS = 5_000L
    }
}
