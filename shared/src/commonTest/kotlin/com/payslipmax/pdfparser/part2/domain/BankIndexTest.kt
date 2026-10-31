package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the officer finds the order he is chasing by date, so the list is in From order, grouped by year, and a waiting row says how long
 * it has waited from an injected clock (never the device clock inside the logic), in the owner's words: days under 30, then whole
 * months, then years and months. The chip counts are the same data, so a chip can never disagree with its list.
 */
class BankIndexTest {
    private val today = date(2026, 10, 30)

    private fun ids(view: BankView) = view.years.flatMap { y -> y.rows.map { it.entry.id } }

    @Test
    fun rowsAreNewestFromFirstWithEqualDatesKeptInTheOrderTheyWereNoted() {
        val view =
            BankIndex.view(
                listOf(
                    entry("old", date(2024, 3, 1)),
                    entry("tieA", date(2025, 6, 1), created = 1),
                    entry("new", date(2026, 2, 1)),
                    entry("tieB", date(2025, 6, 1), created = 2),
                ),
                emptySet(),
                BankFilter.ALL,
                today,
            )
        assertEquals(listOf("new", "tieB", "tieA", "old"), ids(view))
    }

    @Test
    fun yearsGroupAtTheBoundaryByTheFromDate() {
        val view = BankIndex.view(listOf(entry("a", date(2025, 12, 31)), entry("b", date(2026, 1, 1))), emptySet(), BankFilter.ALL, today)
        assertEquals(listOf(2026, 2025), view.years.map { it.year })
        assertEquals(listOf("b"), view.years[0].rows.map { it.entry.id })
        assertEquals(listOf("a"), view.years[1].rows.map { it.entry.id })
    }

    @Test
    fun theChipsFilterByStatusAndTheCountsAreForTheWholeBank() {
        val entries = listOf(entry("w1", date(2026, 1, 1)), entry("a1", date(2026, 2, 1), orderId = "o1"), entry("w2", date(2026, 3, 1), orderId = "gone"))
        val orders = setOf("o1")
        val waiting = BankIndex.view(entries, orders, BankFilter.NOT_ATTACHED, today)
        val attached = BankIndex.view(entries, orders, BankFilter.ATTACHED, today)
        assertEquals(listOf("w2", "w1"), ids(waiting))
        assertEquals(listOf("a1"), ids(attached))
        assertEquals(Triple(3, 2, 1), Triple(waiting.all, waiting.notAttached, waiting.attached))
        assertEquals(3, BankIndex.view(entries, orders, BankFilter.ALL, today).years.sumOf { it.rows.size })
    }

    @Test
    fun onlyAWaitingRowHasAnAge() {
        val view = BankIndex.view(listOf(entry("w", date(2026, 7, 1)), entry("a", date(2026, 7, 1), orderId = "o1")), setOf("o1"), BankFilter.ALL, today)
        val rows = view.years.single().rows.associateBy { it.entry.id }
        assertNull(rows.getValue("a").age)
        assertEquals(WaitingAge.Months(3), rows.getValue("w").age)
    }

    @Test
    fun ageIsDaysUnderThirtyThenWholeMonthsThenYearsAndMonths() {
        val from = date(2026, 1, 1)
        assertEquals(WaitingAge.Days(0), BankIndex.ageOf(from, date(2026, 1, 1)))
        assertEquals(WaitingAge.Days(1), BankIndex.ageOf(from, date(2026, 1, 2)))
        assertEquals(WaitingAge.Days(29), BankIndex.ageOf(from, date(2026, 1, 30)))
        assertEquals(WaitingAge.Months(1), BankIndex.ageOf(from, date(2026, 1, 31)), "30 days is no longer 'days'")
        assertEquals(WaitingAge.Months(1), BankIndex.ageOf(from, date(2026, 2, 1)))
        assertEquals(WaitingAge.Months(5), BankIndex.ageOf(from, date(2026, 6, 30)), "a month is whole only on the same day number")
        assertEquals(WaitingAge.Months(6), BankIndex.ageOf(from, date(2026, 7, 1)))
        assertEquals(WaitingAge.Months(11), BankIndex.ageOf(from, date(2026, 12, 31)))
        assertEquals(WaitingAge.YearsMonths(1, 0), BankIndex.ageOf(from, date(2027, 1, 1)))
        assertEquals(WaitingAge.YearsMonths(1, 2), BankIndex.ageOf(from, date(2027, 3, 5)))
        assertEquals(WaitingAge.YearsMonths(3, 11), BankIndex.ageOf(from, date(2029, 12, 31)))
    }

    @Test
    fun aFromDateInTheFutureHasNoAgeYet() {
        assertNull(BankIndex.ageOf(date(2026, 11, 1), date(2026, 10, 30)))
    }

    @Test
    fun anEmptyBankIsAnEmptyViewWithZeroCounts() {
        val view = BankIndex.view(emptyList(), emptySet(), BankFilter.ALL, today)
        assertEquals(emptyList(), view.years)
        assertEquals(Triple(0, 0, 0), Triple(view.all, view.notAttached, view.attached))
    }
}
