package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the orange and blue labels are the Bank's whole promise, so "waiting" is decided in exactly one place, from whether the order the
 * entry names still exists. Nothing is stored, so deleting an order needs no cascade write and can never leave an entry saying "attached"
 * to a copy that is gone.
 */
class BankStatusTest {
    @Test
    fun anEntryWithNoOrderIsWaiting() {
        assertEquals(BankStatus.NOT_ATTACHED, BankStatus.of(entry("e1", date(2026, 1, 1)), orderIds = setOf("o1")))
    }

    @Test
    fun anEntryWhoseOrderExistsIsAttached() {
        assertEquals(BankStatus.ATTACHED, BankStatus.of(entry("e1", date(2026, 1, 1), orderId = "o1"), orderIds = setOf("o1", "o2")))
    }

    @Test
    fun aDanglingOrderIdReadsAsWaitingSoDeletingAnOrderNeedsNoCascade() {
        val linked = entry("e1", date(2026, 1, 1), orderId = "o1")
        assertEquals(BankStatus.ATTACHED, BankStatus.of(linked, setOf("o1")))
        assertEquals(BankStatus.NOT_ATTACHED, BankStatus.of(linked, emptySet()), "the order was deleted: the same entry is waiting again")
    }
}
