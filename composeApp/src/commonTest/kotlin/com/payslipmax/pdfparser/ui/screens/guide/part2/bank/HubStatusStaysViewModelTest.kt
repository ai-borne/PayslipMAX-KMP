package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the hub's "stays to check" count must follow the encrypted store: a stay that ends with no regularising entry appears, and the
 * regularising entry the officer then adds makes it go away, without the officer doing anything on the hub. The count is real data
 * run through the real rule, so a wrong rule or a stale read shows here.
 */
class HubStatusStaysViewModelTest : BankTestBase() {
    private suspend fun admitAndDischarge() {
        repository.saveEntry(SyntheticBank.entry("adm", code = "HOSADM", from = CalendarDate(2026, 3, 1)))
        repository.saveEntry(SyntheticBank.entry("dis", code = "HOSDISCH", from = CalendarDate(2026, 3, 20)))
    }

    @Test
    fun anEndedStayWithNoRegularisingEntryIsCounted() =
        runTest {
            admitAndDischarge()
            val status = bank().hub.first { it != HubStatus.Unknown } as HubStatus.Filing
            assertEquals(1, status.unregularised)
            assertEquals(0, status.timeBarred)
        }

    @Test
    fun addingTheRegularisingEntryClearsTheCount() =
        runTest {
            admitAndDischarge()
            val model = bank()
            assertEquals(1, (model.hub.first { it != HubStatus.Unknown } as HubStatus.Filing).unregularised)

            repository.saveEntry(SyntheticBank.entry("slc", code = "SLC", from = CalendarDate(2026, 3, 1)))

            val cleared = withTimeoutOrNull(1_000) { model.hub.first { it is HubStatus.Filing && it.unregularised == 0 } }
            assertEquals(0, (cleared as HubStatus.Filing).unregularised)
        }

    @Test
    fun theListsChipDoesNotChangeTheHubsStayCount() =
        runTest {
            admitAndDischarge()
            val model = bank()
            model.setFilter(BankFilter.ATTACHED)
            assertEquals(1, (model.hub.first { it != HubStatus.Unknown } as HubStatus.Filing).unregularised)
        }
}
