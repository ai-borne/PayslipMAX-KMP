package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: saving is where the order, its pages and the links must all happen or none: a refused form writes nothing, a failed save leaves no
 * half-attached order and no plaintext page, and one scan covers exactly the casualties ticked.
 */
class AttachOrderSaveTest : AttachTestBase() {
    private suspend fun waiting(vararg ids: String) = ids.forEach { fake.saveEntry(SyntheticBank.entry(it)) }

    private suspend fun AttachOrderViewModel.ready(vararg tick: String): AttachOrderViewModel {
        begin()
        scan(scanner)
        tick.forEach(::toggle)
        return this
    }

    private suspend fun AttachOrderViewModel.saved(): AttachOutcome {
        var outcome: AttachOutcome? = null
        save { outcome = it }
        return outcome!!
    }

    @Test
    fun savingWithNoPageOrNoCasualtyIsRefusedAndWritesNothing() =
        runTest {
            waiting("e1")
            val vm = attach()
            vm.begin()
            assertEquals(AttachOutcome.Invalid, vm.saved())
            assertEquals(setOf(AttachProblem.NO_PAGES, AttachProblem.NO_CASUALTY), vm.state.value.problems)

            val held = pages("a")
            scans(held)
            vm.scan(scanner)
            assertEquals(AttachOutcome.Invalid, vm.saved())
            assertEquals(setOf(AttachProblem.NO_CASUALTY), vm.state.value.problems)
            assertTrue(fake.observeOrders().first().items.isEmpty())
            assertTrue(held.none { it.discarded }, "a refused save keeps the scan so the officer can fix the form")
        }

    @Test
    fun theTypedNumbersAreBounded() =
        runTest {
            waiting("e1")
            scans(pages("a"))
            val vm = attach().ready("e1")
            vm.setOrderNo("a".repeat(Part2Limits.ORDER_NO_MAX + 1))
            vm.setLastOrderNo("b".repeat(Part2Limits.ORDER_NO_MAX + 1))
            vm.setExpected("0")

            assertEquals(AttachOutcome.Invalid, vm.saved())
            assertEquals(setOf(AttachProblem.ORDER_NO_LONG, AttachProblem.LAST_NO_LONG, AttachProblem.BAD_COUNT), vm.state.value.problems)

            for (bad in listOf("abc", "1000", "-3", "2.5")) {
                vm.setOrderNo("ok")
                vm.setLastOrderNo("ok")
                vm.setExpected(bad)
                assertEquals(AttachOutcome.Invalid, vm.saved(), bad)
            }
        }

    @Test
    fun oneScanCoversEveryTickedCasualtyAndNoOther() =
        runTest {
            waiting("e1", "e2", "e3")
            val held = pages("a", "b")
            scans(held)
            val vm = attach()
            vm.begin()
            vm.scan(scanner)
            vm.toggle("e1")
            vm.toggle("e3")
            vm.setOrderNo(" SYN/77 ")
            vm.setDate(CalendarDate(2026, 3, 20))
            vm.setExpected("5")
            vm.setLastOrderNo("SYN/70")

            val outcome = assertIs<AttachOutcome.Saved>(vm.saved())

            val order = fake.order(outcome.orderId)!!
            assertEquals(Triple("SYN/77", CalendarDate(2026, 3, 20), 5), Triple(order.orderNo, order.orderDate, order.expectedCount))
            assertEquals("SYN/70", order.lastOrderNo)
            assertEquals(2, fake.pages(order.id).size)
            assertEquals(listOf(order.id, null, order.id), listOf("e1", "e2", "e3").map { fake.entry(it)!!.orderId })
            assertEquals(2 to 0, outcome.saved to outcome.notSaved)
            assertTrue(held.all { it.discarded }, "the scanner's copy is gone once the pages are stored")
            assertFalse(vm.holdsScan())
        }

    @Test
    fun aBlankNumberIsNotStoredAsAnEmptyString() =
        runTest {
            waiting("e1")
            scans(pages("a"))
            val vm = attach().ready("e1")

            val outcome = assertIs<AttachOutcome.Saved>(vm.saved())

            val order = fake.order(outcome.orderId)!!
            assertNull(order.orderNo)
            assertNull(order.lastOrderNo)
            assertNull(order.expectedCount)
        }

    @Test
    fun aFailedOrderSaveDiscardsTheScanAndChangesNoEntry() =
        runTest {
            waiting("e1")
            val held = pages("a")
            scans(held)
            val vm = attach().ready("e1")
            fake.orderWriteResult = Part2WriteResult.REJECTED

            assertEquals(AttachOutcome.Failed, vm.saved())

            assertTrue(held.all { it.discarded })
            assertNull(fake.entry("e1")!!.orderId)
            assertTrue(fake.observeOrders().first().items.isEmpty())
        }

    @Test
    fun aFullOrderListIsReportedAsFullAndDiscardsTheScan() =
        runTest {
            waiting("e1")
            val held = pages("a")
            scans(held)
            val vm = attach().ready("e1")
            fake.orderWriteResult = Part2WriteResult.TOO_MANY

            assertEquals(AttachOutcome.BankFull, vm.saved())
            assertTrue(held.all { it.discarded })
        }

    @Test
    fun whenNoPageCanBeStoredTheOrderIsRemovedAndTheEntriesAreUntouched() =
        runTest {
            waiting("e1")
            val held = pages("a")
            scans(held)
            refuseMarker = 1
            val vm = attach().ready("e1")

            assertEquals(AttachOutcome.Failed, vm.saved())

            assertTrue(fake.observeOrders().first().items.isEmpty())
            assertNull(fake.entry("e1")!!.orderId)
            assertTrue(held.all { it.discarded })
        }

    @Test
    fun aPageThatCannotBeStoredIsCountedNotHidden() =
        runTest {
            waiting("e1")
            val good = com.payslipmax.pdfparser.testing.FakeScannedPage(byteArrayOf(9, 9, 9), name = "good")
            val bad = com.payslipmax.pdfparser.testing.FakeScannedPage(byteArrayOf(9, 9, 1), readable = false, name = "bad")
            scans(listOf(good, bad))
            val vm = attach().ready("e1")

            val outcome = assertIs<AttachOutcome.Saved>(vm.saved())

            assertEquals(1 to 1, outcome.saved to outcome.notSaved)
            assertTrue(good.discarded && bad.discarded)
        }

    @Test
    fun ifAnEntryCannotBeLinkedTheOrderAndItsPagesAreRemoved() =
        runTest {
            waiting("e1", "e2")
            scans(pages("a"))
            val real = fake
            repository = FailingEntryRepository(real, failFor = "e2")
            val vm = attach().ready("e1", "e2")

            assertEquals(AttachOutcome.Failed, vm.saved())

            assertTrue(real.observeOrders().first().items.isEmpty())
            val derived = real.observeEntries().first().items.map { it.orderId }
            assertTrue(derived.all { it == null || real.order(it) == null }, "no entry may read as attached to a missing order")
        }

    @Test
    fun choicesAreOnlyTheCasualtiesStillWaitingAndNewOnesAreTicked() =
        runTest {
            waiting("e1")
            fake.saveEntry(SyntheticBank.entry("e2", orderId = "order-x"))
            fake.saveOrder(SyntheticBank.order("order-x"))
            val vm = attach()
            vm.begin()

            assertEquals(listOf("e1"), vm.state.first { it.choices.isNotEmpty() }.choices.map { it.id })
            assertEquals(emptySet(), vm.state.value.ticked)

            waiting("e9")
            assertEquals(setOf("e9"), vm.state.first { "e9" in it.ticked }.ticked)
        }

    @Test
    fun beginningFromOneEntryTicksIt() =
        runTest {
            waiting("e1", "e2")
            val vm = attach()

            vm.begin("e2")

            assertEquals(setOf("e2"), vm.state.first { it.choices.size == 2 }.ticked)
        }

    @Test
    fun aKilledProcessLosesOnlyTheScan() =
        runTest {
            waiting("e1")
            scans(pages("a"))
            val first = attach().ready("e1")
            first.setOrderNo("SYN/1")
            // The process dies here: no close, no save. A new view model on the same store starts blank.

            val second = attach()
            second.begin()

            assertEquals(0, second.state.value.pages)
            assertEquals("", second.state.value.orderNo)
            assertTrue(fake.observeOrders().first().items.isEmpty())
            assertNull(fake.entry("e1")!!.orderId)
        }

    @Test
    fun theStateAndTheOutcomeHideWhatWasTyped() =
        runTest {
            waiting("e1")
            scans(pages("a"))
            val vm = attach().ready("e1")
            vm.setOrderNo(SyntheticBank.MARKER_ORDER_NO)
            vm.setLastOrderNo("SYN-LAST-Qq1")

            val text = vm.state.value.toString()

            assertFalse(text.contains(SyntheticBank.MARKER_ORDER_NO) || text.contains("SYN-LAST-Qq1") || text.contains("e1"), text)
        }

    @Test
    fun pagesTheScannerDroppedAreCountedAsNotSaved() =
        runTest {
            waiting("e1")
            val kept = pages("a")
            scanner.next = PageScan.Pages(kept, dropped = 2)
            val vm = attach().ready("e1")

            val outcome = assertIs<AttachOutcome.Saved>(vm.saved())

            assertEquals(1 to 2, outcome.saved to outcome.notSaved)
        }
}
