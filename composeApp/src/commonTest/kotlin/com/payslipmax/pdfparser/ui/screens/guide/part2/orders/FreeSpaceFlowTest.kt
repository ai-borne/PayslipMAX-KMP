package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.part2.space.FreeSpaceGate
import com.payslipmax.pdfparser.testing.FakePageEncoder
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: free space is checked BEFORE the scanner opens, in both doors to a scan (the attach form and "Add pages"): below the refusal line
 * the camera never starts and nothing is held, below the warning line the scan goes ahead and the officer is told, and the warning
 * goes away once the officer has freed the space. The check must not cost a scan that would have worked.
 */
class FreeSpaceFlowTest : AttachTestBase() {
    private val almostFull = Part2Limits.FREE_SPACE_REFUSE_BYTES - 1
    private val running = Part2Limits.FREE_SPACE_WARN_BYTES - 1

    private fun order() = OrderViewModel(fake, PageStore(FakePageEncoder(), fake, dispatcher, FreeSpaceGate(space)), "o1", dispatcher)

    @Test
    fun attachBelowTheRefusalLineNeverOpensTheScannerAndHoldsNothing() =
        runTest {
            scans(pages("a"))
            space.free = almostFull
            val vm = attach()
            vm.begin()

            vm.scan(scanner)

            assertTrue(scanner.limits.isEmpty(), "the scanner was not opened")
            assertEquals(AttachNotice.NO_SPACE, vm.state.value.notice)
            assertFalse(vm.holdsScan())
            assertEquals(OrderStrings.noSpace(), OrderNotices.of(AttachNotice.NO_SPACE))
        }

    @Test
    fun attachBelowTheWarningLineScansAndWarns() =
        runTest {
            scans(pages("a"))
            space.free = running
            val vm = attach()
            vm.begin()

            vm.scan(scanner)

            assertEquals(1, vm.state.value.pages)
            assertTrue(vm.state.value.lowSpace)
            assertNull(vm.state.value.notice)
        }

    @Test
    fun attachWithPlentyOfSpaceSaysNothing() =
        runTest {
            scans(pages("a"))
            val vm = attach()
            vm.begin()

            vm.scan(scanner)

            assertFalse(vm.state.value.lowSpace)
        }

    @Test
    fun theWarningClearsAtTheNextScanOnceSpaceWasFreed() =
        runTest {
            scans(pages("a"))
            space.free = running
            val vm = attach()
            vm.begin()
            vm.scan(scanner)
            space.free = Part2Limits.FREE_SPACE_WARN_BYTES

            vm.scan(scanner)

            assertFalse(vm.state.value.lowSpace)
        }

    @Test
    fun theFreeSpaceIsReadOnceBeforeTheScannerNotAfterIt() =
        runTest {
            scans(pages("a"))
            val vm = attach()
            vm.begin()

            vm.scan(scanner)

            assertEquals(1, space.reads)
        }

    @Test
    fun addPagesBelowTheRefusalLineNeverOpensTheScanner() =
        runTest {
            fake.saveOrder(SyntheticBank.order("o1"))
            scans(pages("a"))
            space.free = almostFull
            val vm = order()

            vm.addPages(scanner)

            assertTrue(scanner.limits.isEmpty())
            assertEquals(CaptureResult.NoSpace, vm.state.first { it.loaded }.capture)
            assertTrue(fake.pages("o1").isEmpty())
        }

    @Test
    fun addPagesBelowTheWarningLineStoresTheScanAndWarns() =
        runTest {
            fake.saveOrder(SyntheticBank.order("o1"))
            scans(pages("a"))
            space.free = running
            val vm = order()

            vm.addPages(scanner)

            val state = vm.state.first { it.loaded }
            assertEquals(1, state.pages.size)
            assertTrue(state.lowSpace)
        }

    @Test
    fun anOrderThatIsAlreadyFullIsToldSoBeforeAnythingAboutSpace() =
        runTest {
            fake.saveOrder(SyntheticBank.order("o1"))
            repeat(Part2Limits.PAGES_PER_ORDER_MAX) { fake.addPage("o1", SyntheticBank.jpeg(40, it + 1)) }
            space.free = almostFull
            val vm = order()

            vm.addPages(scanner)

            assertEquals(CaptureResult.OrderFull, vm.state.first { it.loaded }.capture)
        }

    @Test
    fun theWordsGiveTheLinesInMegabytes() {
        assertTrue("100 MB" in OrderNotices.of(CaptureResult.NoSpace)!!)
        assertTrue("500 MB" in OrderStrings.lowSpace())
    }
}
