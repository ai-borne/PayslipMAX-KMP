package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.test.Test
import kotlin.test.assertEquals

/** WHY: the shelf is how an officer finds the paper for a casualty by its order, so the grouping and order of rows must be predictable. */
class OrderShelfTest {
    private fun order(
        id: String,
        date: CalendarDate?,
        no: String? = "SYN/$id",
        last: String? = null,
        expected: Int? = null,
    ) = VaultOrder(id, no, date, 10L, 20L, expected, last)

    @Test
    fun yearsRunNewestFirstAndRowsRunNewestDateFirstWithinAYear() {
        val shelf =
            OrderShelf.view(
                listOf(order("a", CalendarDate(2025, 1, 5)), order("b", CalendarDate(2026, 2, 1)), order("c", CalendarDate(2026, 9, 9))),
                emptyList(),
            )

        assertEquals(listOf(2026, 2025), shelf.map { it.year })
        assertEquals(listOf("c", "b"), shelf.first().rows.map { it.order.id })
    }

    @Test
    fun ordersWithNoDateGoLastInTheirOwnGroup() {
        val shelf = OrderShelf.view(listOf(order("u", null), order("d", CalendarDate(2024, 3, 3))), emptyList())

        assertEquals(listOf<Int?>(2024, null), shelf.map { it.year })
    }

    @Test
    fun aRowCarriesItsCounterAndItsChainState() {
        val orders = listOf(order("o1", CalendarDate(2026, 3, 1), expected = 4, last = "SYN/gone"), order("o2", CalendarDate(2026, 2, 1)))
        val entries = listOf(SyntheticBank.entry("e1", orderId = "o1"), SyntheticBank.entry("e2", orderId = "o1"), SyntheticBank.entry("e3", orderId = "o2"))

        val row = OrderShelf.view(orders, entries).single().rows.first { it.order.id == "o1" }

        assertEquals(OrderCompleteness(2, 4), row.completeness)
        assertEquals(ChainState.MISSING, row.chain)
    }
}
