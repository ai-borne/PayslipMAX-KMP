package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves the area re-grant rules are fast on Kotlin/Native's runtime, not just correct on the JVM (plan 12, Cross-cutting checks): the leave
 * form redrafts on every keystroke and each draft asks every one of the 31 area conditions and both Transport Allowance date rules, which walk
 * the calendar. Plain arithmetic and scans only, no regex; this is the automated gate for that.
 */
class AreaRulesIosPerfTest {
    private val values =
        mapOf("prefix_first" to date(6, 23), "leave_from" to date(6, 25), "leave_to" to date(8, 5), "suffix_last" to date(8, 6))
    private val answers = setOf(AreaRules.ANSWER_HAA_PREFIX + "GTDHH11", AreaRules.ANSWER_FIELD_PREFIX + "GTDHAFA", AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN1")

    @Test
    fun everyAreaConditionAndBothTransportDatesAreJudgedTwentyThousandTimesWithinTheBound() {
        val conditions = AreaRules.all.filterIsInstance<ConditionRule>()
        val derives = AreaRules.all.filterIsInstance<DeriveRule>()
        val casual = context("GTDCL", values = values, answers = answers, computed = mapOf("days" to number(42)))
        val request = OccurrenceRequest("GTDAL", values, answers)
        val started = TimeSource.Monotonic.markNow()
        var holding = 0
        var dated = 0
        repeat(20_000) {
            holding += conditions.count { it.holds(casual) }
            dated += derives.count { it.derive(DeriveContext(request, values)) != null }
        }
        val elapsed = started.elapsedNow().inWholeMilliseconds
        assertEquals(20_000 * 4, holding, "haa, field, tran and tran_stop hold; nothing else")
        assertEquals(20_000 * 2, dated)
        assertTrue(elapsed < BOUND_MS, "20,000 passes over the area rules took $elapsed ms")
    }

    private companion object {
        const val BOUND_MS = 5_000L
    }
}
