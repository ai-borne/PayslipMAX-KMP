package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: attaching is where a paper order becomes encrypted pages tied to casualties. A cancel must leave nothing, a failed save must not
 * leave a half-attached order or a plaintext page behind, and one scan must cover every casualty the officer ticked and no other.
 */
class AttachOrderViewModelTest : AttachTestBase() {
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
    fun aScanIsHeldAndOnlyItsCountIsShown() =
        runTest {
            val held = pages("a", "bb")
            scans(held)
            val vm = attach()
            vm.begin()

            vm.scan(scanner)

            assertEquals(2, vm.state.value.pages)
            assertTrue(vm.holdsScan())
            assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX), scanner.limits)
        }

    @Test
    fun aCancelledScanLeavesNothingAndSaysNothing() =
        runTest {
            scanner.next = PageScan.Cancelled
            val vm = attach()
            vm.begin()

            vm.scan(scanner)

            assertEquals(0, vm.state.value.pages)
            assertNull(vm.state.value.notice)
            assertFalse(vm.holdsScan())
        }

    @Test
    fun eachFailureHasItsOwnNotice() =
        runTest {
            val vm = attach()
            vm.begin()

            scanner.next = PageScan.Failed(ScanFailure.UNAVAILABLE)
            vm.scan(scanner)
            assertEquals(AttachNotice.SCAN_UNAVAILABLE, vm.state.value.notice)

            scanner.next = PageScan.Failed(ScanFailure.FAILED)
            vm.scan(scanner)
            assertEquals(AttachNotice.SCAN_FAILED, vm.state.value.notice)
        }

    @Test
    fun moreScansAddPagesUpToTheOrderLimitAndNeverBeyond() =
        runTest {
            val vm = attach()
            vm.begin()
            scans(pages("1", "2", "3", "4", "5", "6"))
            vm.scan(scanner)
            val second = pages("7", "8", "9", "10", "11", "12")
            scans(second)
            vm.scan(scanner)

            assertEquals(Part2Limits.PAGES_PER_ORDER_MAX, vm.state.value.pages)
            assertEquals(listOf(10, 4), scanner.limits)
            assertTrue(second.drop(4).all { it.discarded }, "pages past the limit are deleted at once")

            val before = scanner.limits.size
            vm.scan(scanner)
            assertEquals(before, scanner.limits.size, "a full order does not open the scanner")
            assertEquals(AttachNotice.ORDER_FULL, vm.state.value.notice)
        }

    @Test
    fun discardingTheScanDeletesEveryHeldPage() =
        runTest {
            val held = pages("a", "b")
            scans(held)
            val vm = attach()
            vm.begin()
            vm.scan(scanner)

            vm.discardScan()

            assertTrue(held.all { it.discarded })
            assertEquals(0, vm.state.value.pages)
            assertFalse(vm.holdsScan())
        }

    @Test
    fun closingTheFlowDeletesTheHeldPagesAndTheTypedText() =
        runTest {
            val held = pages("a")
            scans(held)
            val vm = attach().ready()
            vm.setOrderNo("SYN/1")

            vm.close()

            assertTrue(held.all { it.discarded })
            assertEquals("", vm.state.value.orderNo)
            assertEquals(0, vm.state.value.pages)
        }
}
