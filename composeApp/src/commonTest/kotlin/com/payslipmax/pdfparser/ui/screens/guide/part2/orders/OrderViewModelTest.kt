package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.ChainState
import com.payslipmax.pdfparser.part2.domain.OrderCompleteness
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.testing.FakePageEncoder
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the order screen is where an officer checks the paper, adds a forgotten page, or throws the scan away. Deleting an order must send
 * its casualties back to "not attached" without rewriting them, a page can be read only through a call that zeroes it, and the last
 * casualty of an order cannot be unlinked (the order would then cover nobody).
 */
class OrderViewModelTest : AttachTestBase() {
    private suspend fun seeded(pageCount: Int = 2) {
        fake.saveOrder(SyntheticBank.order("o1").copy(expectedCount = 4, lastOrderNo = "SYN/gone"))
        repeat(pageCount) { fake.addPage("o1", SyntheticBank.jpeg(40, it + 1)) }
        fake.saveEntry(SyntheticBank.entry("e1", orderId = "o1"))
        fake.saveEntry(SyntheticBank.entry("e2", orderId = "o1"))
        fake.saveEntry(SyntheticBank.entry("e3", orderId = null))
    }

    private fun order() = OrderViewModel(fake, PageStore(FakePageEncoder(), fake, dispatcher), "o1", dispatcher)

    @Test
    fun theScreenShowsThePagesTheCasualtiesTheCounterAndTheChain() =
        runTest {
            seeded()

            val state = order().state.first { it.loaded }

            assertEquals(2, state.pages.size)
            assertEquals(listOf("e1", "e2"), state.casualties.map { it.id }.sorted())
            assertEquals(OrderCompleteness(2, 4), state.completeness)
            assertEquals(ChainState.MISSING, state.chain)
            assertTrue(state.canUnlink)
        }

    @Test
    fun aPageIsReadOnlyThroughACallThatZeroesTheBytes() =
        runTest {
            seeded(1)
            val vm = order()
            val ref = vm.state.first { it.loaded }.pages.single()
            var seen: ByteArray? = null

            val size = vm.page(ref.id) { bytes -> bytes.also { seen = it }.size }

            assertEquals(40, size)
            assertTrue(seen!!.all { it == 0.toByte() }, "the plaintext page is overwritten once the caller is done")
            assertNull(vm.page("no-such-page") { it.size })
        }

    @Test
    fun deletingAPageKeepsTheRestAndTheOrder() =
        runTest {
            seeded()
            val vm = order()
            val first = vm.state.first { it.loaded }.pages.first()

            vm.deletePage(first.id)

            val after = vm.state.first { it.pages.size == 1 }
            assertFalse(after.pages.any { it.id == first.id })
            assertTrue(fake.order("o1") != null)
        }

    @Test
    fun deletingTheOrderRemovesItsPagesAndSendsItsCasualtiesBackWithoutRewritingThem() =
        runTest {
            seeded()
            val before = fake.entry("e1")!!
            var done = false

            order().deleteOrder { done = true }

            assertTrue(done)
            assertNull(fake.order("o1"))
            assertTrue(fake.pages("o1").isEmpty())
            assertEquals(before, fake.entry("e1"), "no write to the entry: its status is derived")
            val orders = fake.observeOrders().first().items.map { it.id }.toSet()
            assertEquals(BankStatus.NOT_ATTACHED, BankStatus.of(fake.entry("e1")!!, orders))
            assertEquals(2, fake.observeEntries().first().items.count { BankStatus.of(it, orders) == BankStatus.NOT_ATTACHED && it.id != "e3" })
        }

    @Test
    fun oneCasualtyCanBeUnlinkedButNotTheLast() =
        runTest {
            seeded()
            val vm = order()
            vm.state.first { it.loaded }

            vm.unlink("e1")
            val after = vm.state.first { it.casualties.size == 1 }
            assertNull(fake.entry("e1")!!.orderId)
            assertFalse(after.canUnlink)

            vm.unlink("e2")
            assertEquals("o1", fake.entry("e2")!!.orderId, "the last casualty stays; delete the order instead")
        }

    @Test
    fun addingPagesStopsAtTheOrderLimit() =
        runTest {
            seeded(Part2Limits.PAGES_PER_ORDER_MAX - 1)
            val vm = order()
            vm.state.first { it.loaded }
            val held = pages("a", "bb", "ccc")
            scans(held)

            vm.addPages(scanner)

            assertEquals(listOf(1), scanner.limits)
            assertEquals(Part2Limits.PAGES_PER_ORDER_MAX, fake.pages("o1").size)
            assertTrue(held.all { it.discarded })
            val result = assertIs<CaptureResult.Saved>(vm.state.value.capture)
            assertEquals(1, result.saved)
        }

    @Test
    fun aFullOrderDoesNotOpenTheScanner() =
        runTest {
            seeded(Part2Limits.PAGES_PER_ORDER_MAX)
            val vm = order()
            vm.state.first { it.loaded }

            vm.addPages(scanner)

            assertTrue(scanner.limits.isEmpty())
            assertEquals(CaptureResult.OrderFull, vm.state.value.capture)
        }

    @Test
    fun anOrderThatIsGoneEndsUpNotLoadedAsAnOrder() =
        runTest {
            val vm = order()

            val state = vm.state.first { it.loaded }

            assertNull(state.order)
            assertContentEquals(emptyList(), state.pages)
        }
}
