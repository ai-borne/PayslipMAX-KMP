package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * WHY: the JVM proves the Bank list is correct, not that it is fast on Kotlin/Native. A full Bank (2,000 entries, a thousand orders) is
 * ordered, grouped and aged on every change of the list, so it must stay far inside what the officer notices. Best of three, because the
 * simulator is noisy.
 */
class BankIndexIosPerfTest {
    private val today = CalendarDate(2026, 10, 30)

    private fun entries() =
        List(Part2Limits.ENTRIES_MAX) { i ->
            BankEntry("e$i", "CODE${i % 40}", CalendarDate(2000 + i % 26, 1 + i % 12, 1 + i % 28), null, listOf("", "", "", ""), "", if (i % 2 == 0) "o${i % 1000}" else null, i.toLong(), i.toLong())
        }

    @Test
    fun aFullBankIsListedWithinTheBudgetOnKotlinNative() {
        val all = entries()
        val orders = (0 until Part2Limits.ORDERS_MAX).map { "o$it" }.toSet()
        val best =
            (1..3).minOf {
                val start = TimeSource.Monotonic.markNow()
                val view = BankIndex.view(all, orders, BankFilter.ALL, today)
                assertEquals(Part2Limits.ENTRIES_MAX, view.all)
                start.elapsedNow().inWholeMilliseconds
            }

        println("PART2-BANK-INDEX ios entries=${Part2Limits.ENTRIES_MAX} bestMs=$best")
        assertTrue(best < BUDGET_MS, "listing a full Bank took $best ms (best of three), budget $BUDGET_MS ms")
    }

    private companion object {
        const val BUDGET_MS = 1_500L
    }
}
