package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * Proves the planner is fast on Kotlin/Native's runtime, not just correct on the JVM. The device holds at most `Part2Limits.DRAFTS_MAX`
 * drafts and `ORDERS_MAX` orders, and the Due list rebuilds the plan on every change, so a full device (1,000 of each, every tenth draft a
 * cancel slip that is checked against every order linked to a draft) must plan well inside a human pause, five times over. The planner
 * uses plain scans and maps only (no regex); this is the automated gate for that, as `Part2EngineIosPerfTest` is for the engine.
 */
class ReminderPlannerIosPerfTest : ReminderTestBase() {
    private fun fullDevice(): Pair<List<Part2Draft>, List<VaultOrder>> {
        val drafts =
            (0 until Part2Limits.DRAFTS_MAX).map { i ->
                val printedAt = at(date(2026, 9, 1)) + i * 1_000L
                if (i % 10 == 9) {
                    cancelSlip("d$i", "CANCEL", "n${i - 1}", date(2026, 7, 1), printedAt)
                } else {
                    draft("d$i", leave(date(2026, 10, 5), date(2026, 10, 5 + i % 20)), printedAt = printedAt)
                }
            }
        val orders = (0 until Part2Limits.ORDERS_MAX).map { i -> order("o$i", "d$i", no = "n$i", on = date(2026, 7, 1)) }
        return drafts to orders
    }

    @Test
    fun aFullDevicePlansFiveTimesWithinTheBound() {
        val (drafts, orders) = fullDevice()
        val mark = TimeSource.Monotonic.markNow()
        var reminders = 0
        repeat(RUNS) {
            val plan = plan(drafts, orders, now = at(date(2026, 10, 20)))
            reminders += plan.due.size + plan.upcoming.size
        }
        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
        assertTrue(reminders > 0, "the full device must give reminders, or the test measures nothing")
        assertTrue(elapsedMs < BOUND_MS, "$RUNS plans of ${drafts.size} drafts took ${elapsedMs}ms, expected < ${BOUND_MS}ms")
    }

    private companion object {
        const val RUNS = 5

        // The JVM plans a full device in well under a second. Generous for a simulator, still far below any quadratic blow-up.
        const val BOUND_MS = 5_000L
    }
}
