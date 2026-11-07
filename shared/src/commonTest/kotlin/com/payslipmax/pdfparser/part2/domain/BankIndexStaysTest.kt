package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer's hospital stays that ended with no regularising entry get their own quiet chip. The chip must list exactly those
 * admissions, its count must come from the same data as the list, and adding it must not change what the three older chips show: an
 * existing officer's Bank must look exactly as before unless a stay needs a look.
 */
class BankIndexStaysTest {
    private val today = date(2026, 10, 30)

    private fun e(
        id: String,
        code: String,
        from: com.payslipmax.pdfparser.guide.preclaim.CalendarDate,
        to: com.payslipmax.pdfparser.guide.preclaim.CalendarDate? = null,
        orderId: String? = null,
    ) = BankEntry(id, code, from, to, listOf("", "", "", ""), "", orderId, 0, 0)

    private val noted =
        listOf(
            e("adm", "HOSADM", date(2026, 3, 1)),
            e("dis", "HOSDISCH", date(2026, 3, 20)),
            e("other", "FIRSTCODE", date(2026, 4, 1), orderId = "o1"),
        )

    private fun ids(view: BankView) = view.years.flatMap { y -> y.rows.map { it.entry.id } }

    @Test
    fun theStaysFilterShowsExactlyTheUnregularisedAdmissions() {
        val view = BankIndex.view(noted, setOf("o1"), BankFilter.UNREGULARISED, today)
        assertEquals(listOf("adm"), ids(view))
        assertEquals(1, view.unregularised)
    }

    @Test
    fun aRegularisingEntryRemovesTheStayFromTheFilterAndTheCount() {
        val view = BankIndex.view(noted + e("slc", "SLC", date(2026, 3, 1), date(2026, 3, 20)), setOf("o1"), BankFilter.UNREGULARISED, today)
        assertEquals(emptyList(), ids(view))
        assertEquals(0, view.unregularised)
    }

    @Test
    fun theCountsCoverTheWholeBankWhateverFilterIsChosen() {
        BankFilter.entries.forEach { filter ->
            val view = BankIndex.view(noted, setOf("o1"), filter, today)
            assertEquals(1, view.unregularised, "$filter")
            assertEquals(0, view.timeBarred, "$filter")
        }
    }

    @Test
    fun aStayOverTwoYearsOldIsCountedAsTimeBarred() {
        val old = listOf(e("adm", "HOSADM", date(2024, 3, 1)), e("dis", "HOSDISCH", date(2024, 3, 20)))
        val view = BankIndex.view(old, emptySet(), BankFilter.ALL, today)
        assertEquals(1, view.unregularised)
        assertEquals(1, view.timeBarred)
    }

    @Test
    fun theThreeOlderFiltersShowWhatTheyAlwaysDidWithHospitalEntriesPresent() {
        // characterization: the new filter must not change the old ones
        assertEquals(listOf("other", "dis", "adm"), ids(BankIndex.view(noted, setOf("o1"), BankFilter.ALL, today)))
        assertEquals(listOf("dis", "adm"), ids(BankIndex.view(noted, setOf("o1"), BankFilter.NOT_ATTACHED, today)))
        assertEquals(listOf("other"), ids(BankIndex.view(noted, setOf("o1"), BankFilter.ATTACHED, today)))
        val view = BankIndex.view(noted, setOf("o1"), BankFilter.ALL, today)
        assertEquals(Triple(3, 2, 1), Triple(view.all, view.notAttached, view.attached))
    }

    @Test
    fun aBankWithNoHospitalEntriesHasNoStaysToCheck() {
        val view = BankIndex.view(listOf(e("x", "FIRSTCODE", date(2026, 4, 1))), emptySet(), BankFilter.UNREGULARISED, today)
        assertEquals(emptyList(), ids(view))
        assertEquals(0, view.unregularised)
    }
}
