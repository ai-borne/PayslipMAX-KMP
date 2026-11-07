package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * WHY: the hub's card must say how many hospital stays need a look using the same numbers as the Bank list, and must stay numbers only
 * (no code, date or id) so it is safe in a screenshot. Existing callers that know nothing about stays must keep compiling and meaning
 * the same thing.
 */
class HubStatusStaysTest {
    private val today = date(2026, 10, 30)

    private fun hospital(from: Int) =
        listOf(
            BankEntry("a$from", "HOSADM", date(from, 3, 1), null, listOf("", "", "", ""), "", null, 0, 0),
            BankEntry("d$from", "HOSDISCH", date(from, 3, 20), null, listOf("", "", "", ""), "", null, 0, 0),
        )

    @Test
    fun filingCarriesTheStayCountsFromTheView() {
        val view = BankIndex.view(hospital(2026) + hospital(2024), emptySet(), BankFilter.ALL, today)
        assertEquals(HubStatus.Filing(4, 0, 4, unregularised = 2, timeBarred = 1), HubStatus.of(true, view))
    }

    @Test
    fun theStayCountsDefaultToZeroSoOlderCallersAreUnchanged() {
        assertEquals(HubStatus.Filing(3, 1, 2), HubStatus.Filing(3, 1, 2, unregularised = 0, timeBarred = 0))
    }

    @Test
    fun emptyAndUnknownCarryNoStayCounts() {
        assertEquals(HubStatus.Empty, HubStatus.of(true, BankIndex.view(emptyList(), emptySet(), BankFilter.ALL, today)))
        assertEquals(HubStatus.Unknown, HubStatus.of(false, BankIndex.view(hospital(2026), emptySet(), BankFilter.ALL, today)))
    }

    @Test
    fun theChipFilterDoesNotChangeTheStayCounts() {
        val view = BankIndex.view(hospital(2026), emptySet(), BankFilter.ATTACHED, today)
        assertEquals(1, (HubStatus.of(true, view) as HubStatus.Filing).unregularised)
    }

    @Test
    fun theStatusStaysFreeOfCodesDatesAndIds() {
        val text = HubStatus.of(true, BankIndex.view(hospital(2026), emptySet(), BankFilter.ALL, today)).toString()
        assertFalse(listOf("HOSADM", "2026", "a2026").any { text.contains(it) }, text)
    }
}
