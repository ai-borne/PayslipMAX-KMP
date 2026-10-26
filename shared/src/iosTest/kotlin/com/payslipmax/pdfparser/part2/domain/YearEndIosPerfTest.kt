package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves the year-end arithmetic and the break-up reading are fast on Kotlin/Native's runtime, not just correct on the JVM (plan 12,
 * Cross-cutting checks): the form redrafts on every change, and a break-up is read, summed and written back each time. Plain scans only,
 * no regex, and this is the automated gate for that.
 */
class YearEndIosPerfTest {
    private val sixtyRows = (0 until Part2Limits.BREAKUP_ROWS_MAX).joinToString("\n") { "${1960 + it} 9999" }

    @Test
    fun aFullBreakUpIsReadSummedAndListedFiveThousandTimesWithinTheBound() {
        val started = TimeSource.Monotonic.markNow()
        var total = 0
        repeat(5_000) {
            val rows = (YearlyBreakup.parse(sixtyRows) as BreakupRead.Rows).rows
            total += YearlyBreakup.total(rows) + YearlyBreakup.format(rows).length
        }
        val elapsed = started.elapsedNow().inWholeMilliseconds
        assertEquals(5_000 * (60 * 9999 + sixtyRows.length), total)
        assertTrue(elapsed < BOUND_MS, "5,000 full break-ups took $elapsed ms")
    }

    @Test
    fun theAccumulationRulesWorkTenThousandSetsOfFiguresWithinTheBound() {
        val started = TimeSource.Monotonic.markNow()
        val figures = mapOf("year" to SlotValue.NumberValue(2024), "prev_total" to SlotValue.NumberValue(380), "ltc_days" to SlotValue.NumberValue(10), "accrued" to SlotValue.NumberValue(19))
        var length = 0
        repeat(10_000) {
            for (rule in AcclveRules.all.filterIsInstance<DeriveRule>()) length += (rule.derive(DeriveContext(OccurrenceRequest("X", figures), figures)) as? SlotValue.TextValue)?.text?.length ?: 0
        }
        val elapsed = started.elapsedNow().inWholeMilliseconds
        assertTrue(length > 0)
        assertTrue(elapsed < BOUND_MS, "10,000 sets of figures took $elapsed ms")
    }

    private companion object {
        const val BOUND_MS = 5_000L
    }
}
