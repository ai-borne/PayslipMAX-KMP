package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** "Last Part II order = the vault order with the latest order date; blank when the vault is empty" (plan 12, Architecture). */
class VaultOrdersTest {
    private fun order(
        id: String,
        no: String?,
        date: CalendarDate?,
        updatedAt: Long = 1L,
    ) = VaultOrder(id, null, no, date, 0L, updatedAt)

    @Test
    fun anEmptyVaultHasNoLastOrder() {
        assertNull(VaultOrders.lastOrder(emptyList()))
    }

    @Test
    fun theLatestOrderDateWinsWhateverTheListOrder() {
        val a = order("a", "N/1", CalendarDate(2026, 3, 1))
        val b = order("b", "N/2", CalendarDate(2026, 5, 9))
        val c = order("c", "N/3", CalendarDate(2025, 12, 31))
        assertEquals(OrderRef("N/2", CalendarDate(2026, 5, 9)), VaultOrders.lastOrder(listOf(a, b, c)))
        assertEquals(OrderRef("N/2", CalendarDate(2026, 5, 9)), VaultOrders.lastOrder(listOf(c, b, a)))
    }

    @Test
    fun aScannedOrderWithoutItsNumberOrDateIsNotYetAnOrderToQuote() {
        val unnumbered = order("a", null, CalendarDate(2026, 9, 9))
        val undated = order("b", "N/9", null)
        val blank = order("c", "  ", CalendarDate(2026, 9, 9))
        val real = order("d", "N/1", CalendarDate(2026, 1, 1))
        assertEquals(OrderRef("N/1", CalendarDate(2026, 1, 1)), VaultOrders.lastOrder(listOf(unnumbered, undated, blank, real)))
        assertNull(VaultOrders.lastOrder(listOf(unnumbered, undated, blank)))
    }

    @Test
    fun twoOrdersOnTheSameDateGiveTheOneEditedLast() {
        val earlier = order("a", "N/1", CalendarDate(2026, 3, 1), updatedAt = 10)
        val later = order("b", "N/2", CalendarDate(2026, 3, 1), updatedAt = 20)
        assertEquals("N/2", VaultOrders.lastOrder(listOf(earlier, later))?.orderNo)
        assertEquals("N/2", VaultOrders.lastOrder(listOf(later, earlier))?.orderNo)
    }

    @Test
    fun aDateThatIsNotOnTheCalendarIsIgnored() {
        val impossible = order("a", "N/1", CalendarDate(2026, 2, 31))
        assertNull(VaultOrders.lastOrder(listOf(impossible)))
    }

    @Test
    fun anOrderNumberTheSlipCouldNotQuoteIsNotQuoted() {
        // why: a quoted last order the engine refuses would refuse every draft, with nothing the officer could fix on the form.
        val long = order("a", "n".repeat(Part2Limits.ORDER_NO_MAX + 1), CalendarDate(2026, 9, 9))
        val markup = order("b", "N<1>", CalendarDate(2026, 9, 9))
        val real = order("c", "N/1", CalendarDate(2026, 1, 1))
        assertEquals("N/1", VaultOrders.lastOrder(listOf(long, markup, real))?.orderNo)
        assertNull(VaultOrders.lastOrder(listOf(long, markup)))
    }
}
