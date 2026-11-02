package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the Part II hub tells the officer where his file stands. It must not announce an empty file before the first read finished (a
 * returning officer would see "Start your Part II file" flash up), and its counts must be the Bank's own counts (one source of truth), so
 * the hub and the Bank list can never disagree about how many order copies are still missing.
 */
class HubStatusTest {
    private val today = date(2026, 10, 30)

    private fun view(vararg entries: BankEntry) = BankIndex.view(entries.toList(), setOf("o1"), BankFilter.ALL, today)

    @Test
    fun beforeTheFirstReadNothingIsSaidEvenThoughTheViewIsEmpty() {
        assertEquals(HubStatus.Unknown, HubStatus.of(loaded = false, view = view()))
    }

    @Test
    fun aLoadedBankWithNoEntryIsEmpty() {
        assertEquals(HubStatus.Empty, HubStatus.of(loaded = true, view = view()))
    }

    @Test
    fun entriesGiveTheBanksOwnCountsForAllAttachedAndNotAttached() {
        val status = HubStatus.of(loaded = true, view = view(entry("a", date(2026, 1, 1), orderId = "o1"), entry("b", date(2026, 2, 1)), entry("c", date(2026, 3, 1))))

        assertEquals(HubStatus.Filing(total = 3, attached = 1, notAttached = 2), status)
    }

    @Test
    fun aFileWithEveryCopyAttachedHasNothingMissing() {
        val status = HubStatus.of(loaded = true, view = view(entry("a", date(2026, 1, 1), orderId = "o1")))

        assertEquals(HubStatus.Filing(total = 1, attached = 1, notAttached = 0), status)
    }

    @Test
    fun theFilterOnTheListNeverChangesTheHubsCounts() {
        val entries = listOf(entry("a", date(2026, 1, 1), orderId = "o1"), entry("b", date(2026, 2, 1)))
        val filtered = BankIndex.view(entries, setOf("o1"), BankFilter.NOT_ATTACHED, today)

        assertEquals(HubStatus.Filing(total = 2, attached = 1, notAttached = 1), HubStatus.of(loaded = true, view = filtered))
    }
}
