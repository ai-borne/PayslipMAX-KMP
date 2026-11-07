package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the counter, the chain flag and the link rules are what tell an officer whether the paper they scanned is whole. A counter that
 * counts another order's casualties, a chain flag that fires for a missing quote, or an order that covers nobody would each mislead them.
 */
class OrderRulesTest {
    private fun order(
        id: String = "o1",
        no: String? = "SYN/10",
        expected: Int? = null,
        last: String? = null,
    ) = VaultOrder(id, no, null, 1L, 2L, expected, last)

    private fun entry(
        id: String,
        orderId: String?,
    ) = SyntheticBank.entry(id = id, orderId = orderId)

    @Test
    fun theCounterCountsOnlyTheCasualtiesOfThisOrder() {
        val entries = listOf(entry("a", "o1"), entry("b", "o1"), entry("c", "o2"), entry("d", null))

        assertEquals(OrderCompleteness(2, 5), OrderCompleteness.of(order(expected = 5), entries))
    }

    @Test
    fun anUnknownCountKeepsTheEnteredNumberOnly() {
        val counter = OrderCompleteness.of(order(expected = null), listOf(entry("a", "o1")))

        assertEquals(1, counter.entered)
        assertNull(counter.expected)
    }

    @Test
    fun noQuoteMeansNoFlagEvenWhenNothingElseIsInTheBank() {
        assertEquals(ChainState.NO_QUOTE, ChainCheck.of(order(last = null), emptyList()))
        assertEquals(ChainState.NO_QUOTE, ChainCheck.of(order(last = "   "), emptyList()))
    }

    @Test
    fun aQuotedNumberThatMatchesAnotherOrderIsFoundIgnoringCaseAndSpaces() {
        val others = listOf(order("o2", no = "syn/9 "))

        assertEquals(ChainState.FOUND, ChainCheck.of(order(last = "  SYN/9"), others))
    }

    @Test
    fun aQuotedNumberThatMatchesNoOtherOrderIsMissing() {
        assertEquals(ChainState.MISSING, ChainCheck.of(order(last = "SYN/9"), listOf(order("o2", no = "SYN/8"), order("o3", no = null))))
    }

    @Test
    fun anOrderQuotingItsOwnNumberDoesNotCountAsItsOwnPredecessor() {
        assertEquals(ChainState.MISSING, ChainCheck.of(order("o1", no = "SYN/10", last = "SYN/10"), listOf(order("o1", no = "SYN/10"))))
    }

    @Test
    fun anOrderNeedsAtLeastOneCasualtyToBeSaved() {
        assertFalse(OrderLink.canSave(emptySet()))
        assertTrue(OrderLink.canSave(setOf("a")))
    }

    @Test
    fun oneScanCoversEveryTickedCasualtyAndNoOther() {
        val entries = listOf(entry("a", null), entry("b", null), entry("c", "o9"))

        val after = OrderLink.attach(entries, setOf("a", "b"), "o1", now = 77L)

        assertEquals(listOf("o1", "o1", "o9"), after.map { it.orderId })
        assertEquals(listOf(77L, 77L, entries[2].updatedAt), after.map { it.updatedAt })
    }

    @Test
    fun theLastCasualtyCannotBeUnlinkedButOneOfSeveralCan() {
        assertFalse(OrderLink.canUnlink("o1", listOf(entry("a", "o1"), entry("b", "o2"))))
        assertTrue(OrderLink.canUnlink("o1", listOf(entry("a", "o1"), entry("b", "o1"))))
    }

    @Test
    fun unlinkingClearsTheOrderAndNothingElse() {
        val before = entry("a", "o1")

        val after = OrderLink.detach(before, now = 99L)

        assertNull(after.orderId)
        assertEquals(before.copy(orderId = null, updatedAt = 99L), after)
    }
}
