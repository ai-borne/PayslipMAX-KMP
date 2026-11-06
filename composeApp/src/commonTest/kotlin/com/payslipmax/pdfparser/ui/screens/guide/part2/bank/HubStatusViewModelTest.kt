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
 * WHY: the hub's top card must follow the encrypted store, not a copy of it. A first entry turns "Start your Part II Orders file" into the
 * file's counts at once, and an attached order copy lowers the "not attached" count, so the officer sees the effect of what he just did
 * when he comes back. The list's chip never changes these counts.
 */
class HubStatusViewModelTest : BankTestBase() {
    @Test
    fun anEmptyStoreIsEmptyOnceReadAndNotBefore() =
        runTest {
            assertEquals(HubStatus.Empty, bank().hub.first { it != HubStatus.Unknown })
        }

    @Test
    fun savingTheFirstEntryTurnsEmptyIntoTheFilesCounts() =
        runTest {
            val model = bank()
            assertEquals(HubStatus.Empty, model.hub.first { it != HubStatus.Unknown })

            repository.saveEntry(SyntheticBank.entry("e1", from = CalendarDate(2026, 4, 1)))

            val after = withTimeoutOrNull(1_000) { model.hub.first { it is HubStatus.Filing } }
            assertEquals(HubStatus.Filing(total = 1, attached = 0, notAttached = 1), after)
        }

    @Test
    fun attachingAnOrderCopyMovesOneFromNotAttachedToAttached() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("e1", from = CalendarDate(2026, 4, 1)))
            repository.saveEntry(SyntheticBank.entry("e2", from = CalendarDate(2026, 5, 1)))
            val model = bank()
            assertEquals(HubStatus.Filing(2, 0, 2), model.hub.first { it != HubStatus.Unknown })

            repository.saveOrder(SyntheticBank.order("o1"))
            repository.saveEntry(SyntheticBank.entry("e1", orderId = "o1", from = CalendarDate(2026, 4, 1)))

            assertEquals(HubStatus.Filing(2, 1, 1), withTimeoutOrNull(1_000) { model.hub.first { it == HubStatus.Filing(2, 1, 1) } })
        }

    @Test
    fun theListsChipDoesNotChangeTheHubsCounts() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("e1", from = CalendarDate(2026, 4, 1)))
            val model = bank()
            model.setFilter(BankFilter.ATTACHED)

            assertEquals(HubStatus.Filing(1, 0, 1), model.hub.first { it != HubStatus.Unknown })
        }
}
