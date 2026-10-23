package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves the engine is fast on Kotlin/Native's runtime, not just correct on the JVM: the Leave release ships 59 templates and the
 * officer's form redrafts on every change, so 200 drafts of every template must finish well inside a human pause. The engine uses
 * plain scans and maps only (no regex), and this is the automated gate for that, as `ParserUtilsIosPerfTest` is for the parser.
 */
class Part2EngineIosPerfTest {
    private val templateCount = 59
    private val draftsPerTemplate = 200

    private fun synthetic(index: Int): Part2Template {
        val code = "PERF$index"
        return Part2Template(
            code = code,
            rev = "0000000$index".takeLast(8),
            page = 28,
            narrative = "Granted the thing $index for {days} days from {from_$index} to {to_$index} vide {grant.no} dated {grant.date} as {remark}",
            slots =
                listOf(
                    Part2Slot("from_$index", "date", "From", "The first day."),
                    Part2Slot("to_$index", "date", "To", "The last day."),
                    Part2Slot("days", "days", "Days", "Counted for you.", listOf("from_$index", "to_$index")),
                    Part2Slot("grant", "ref", "Grant", "The grant order."),
                    Part2Slot("remark", "text", "Remark", "Plain words."),
                ),
            from = Part2Column("required", "from_$index"),
            to = Part2Column("required", "to_$index"),
            data = listOf(Part2Column("required", "days"), Part2Column("optional", "grant.no"), Part2Column("none"), Part2Column("none")),
            rules = listOf(SyntheticPart2Templates.CAP_DAYS),
        )
    }

    @Test
    fun fiftyNineTemplatesTwoHundredDraftsEachFinishWithinTheBound() {
        val templates = (1..templateCount).map(::synthetic)
        val lookup =
            SyntheticPart2Templates.lookup.copy(
                serials = templates.mapIndexed { i, t -> Part2CodePage("9.${i + 1}", 9, listOf(t.code), "A", "Perf ${t.code}", 28) },
                ribbons = emptyList(),
                templates = templates,
            )
        val engine = Part2DraftEngine(lookup, SyntheticPart2Templates.registry())
        val mark = TimeSource.Monotonic.markNow()
        var drafted = 0
        for (template in templates) {
            val index = template.code.removePrefix("PERF")
            val values =
                mapOf(
                    "from_$index" to SlotValue.DateValue(CalendarDate(2026, 7, 5)),
                    "to_$index" to SlotValue.DateValue(CalendarDate(2026, 9, 4)),
                    "remark" to SlotValue.TextValue("plain words"),
                )
            repeat(draftsPerTemplate) {
                val result = engine.draft(LeaveEventInput(listOf(OccurrenceRequest(template.code, values))))
                assertEquals(1, assertIs<DraftResult.Drafted>(result).slip.orders.size)
                drafted++
            }
        }
        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
        assertEquals(templateCount * draftsPerTemplate, drafted)
        assertTrue(elapsedMs < BOUND_MS, "$drafted drafts took ${elapsedMs}ms, expected < ${BOUND_MS}ms")
    }

    private companion object {
        // 11,800 drafts; the JVM does them in well under a second. Generous for a simulator, still far below any quadratic blow-up.
        const val BOUND_MS = 5_000L
    }
}
