package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: an order holds up to ten scanned pages. More can be added until it is full, every outcome of a scan is told on the order's screen, and
 * a page is decrypted only to be shown: the bytes are zeroed when the viewer is done and a page of another order is never read through this one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultOrderPagesTest : VaultOrderTestBase() {
    @Test
    fun scanningMorePagesAddsThemAndTellsTheListAndSaysHowManyWereSaved() =
        check { d ->
            stored(pages = 1)
            val vault = model(d)
            val scan = scanner(pages(2))

            vault.addPages(scan)
            advanceUntilIdle()

            assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX - 1), scan.limits, "the scanner is asked for the room left")
            assertEquals(3, vault.state.value.pages.size)
            assertEquals(listOf<String?>(Part2VaultStrings.pagesSaved(2)), notices)
            assertEquals(1, changes)
        }

    @Test
    fun aFullOrderIsToldSoAndTheScannerIsNotOpened() =
        check { d ->
            stored(pages = Part2Limits.PAGES_PER_ORDER_MAX)
            val vault = model(d)
            val scan = scanner(pages(1))
            assertFalse(vault.state.value.canAddPages)

            vault.addPages(scan)
            advanceUntilIdle()

            assertEquals(emptyList(), scan.limits)
            assertEquals(listOf<String?>(Part2VaultStrings.orderFull()), notices)
        }

    @Test
    fun aScannerThatNeedsAConnectionIsExplainedOnTheOrderScreen() =
        check { d ->
            stored()
            val vault = model(d)
            vault.addPages(scanner(PageScan.Failed(com.payslipmax.pdfparser.part2.pages.ScanFailure.UNAVAILABLE)))
            advanceUntilIdle()
            assertEquals(listOf<String?>(Part2VaultStrings.scanNeedsConnection), notices)
        }

    @Test
    fun aCancelledScanClearsTheOldNoticeAndAddsNone() =
        check { d ->
            stored()
            val vault = model(d)
            vault.addPages(scanner(PageScan.Cancelled))
            advanceUntilIdle()
            assertEquals(listOf<String?>(null), notices, "the screen is told there is nothing to show, so an old notice goes")
        }

    @Test
    fun anOrderDeletedMidScanIsReportedGoneNotCrashed() =
        check { d ->
            stored()
            val vault = model(d)
            val open = OpenScanner()
            vault.addPages(open)
            advanceUntilIdle()
            assertTrue(vault.state.value.busy)
            repo.deleteOrder("order-1")

            open.finish(pages(1))
            advanceUntilIdle()

            assertEquals(listOf<String?>(Part2VaultStrings.orderGone), notices)
            assertEquals(VaultOrderStatus.GONE, vault.state.value.status, "the screen leaves an order that is no longer there")
        }

    @Test
    fun aSecondTapWhileAScanIsOpenIsIgnored() =
        check { d ->
            stored()
            val vault = model(d)
            val open = OpenScanner()
            vault.addPages(open)
            vault.addPages(open)
            advanceUntilIdle()
            assertEquals(1, open.scans)
            open.finish(PageScan.Cancelled)
            advanceUntilIdle()
        }

    @Test
    fun aPageIsHandedToTheViewerAndTheBytesAreZeroedWhenTheViewerIsDone() =
        check { d ->
            stored(pages = 1)
            val vault = model(d)
            val id = vault.state.value.pages.single().id
            var handed: ByteArray? = null

            val size =
                vault.view(id) { bytes ->
                    handed = bytes
                    assertTrue(bytes.any { it != 0.toByte() }, "the viewer is given the real page")
                    bytes.size
                }

            assertEquals(200, size)
            assertTrue(assertNotNull(handed).all { it == 0.toByte() }, "no plaintext page outlives the viewer")
        }

    @Test
    fun aPageOfAnotherOrderIsNeverReadThroughThisOne() =
        check { d ->
            stored(pages = 1)
            stored("order-2", pages = 1)
            val vault = model(d)
            val other = repo.pages("order-2").single().id
            assertNull(vault.view(other) { it.size }, "an order's screen reads only its own pages")
        }

    @Test
    fun aPageThatIsGoneOrCannotBeDecodedIsNull() =
        check { d ->
            stored(pages = 1)
            val vault = model(d)
            assertNull(vault.view("no-such-page") { it.size })
            assertNull(vault.view<Int>(vault.state.value.pages.single().id) { null })
        }

    @Test
    fun aScanBeforeTheOrderHasLoadedIsIgnored() =
        check { d ->
            stored()
            val vault = VaultOrderViewModel("order-1", repo, encoder, { notices += it }, { changes++ }, { 0L }, d)
            val scan = scanner(pages(1))
            vault.addPages(scan)
            advanceUntilIdle()
            assertEquals(emptyList(), scan.limits)
        }

    @Test
    fun aScanAfterTheScreenWasLeftIsIgnored() =
        check { d ->
            stored()
            val vault = model(d)
            vault.close()
            val scan = scanner(pages(1))
            vault.addPages(scan)
            advanceUntilIdle()
            assertEquals(emptyList(), scan.limits)
        }

    @Test
    fun theScreenIsBusyWhileAScanIsOpenAndFreeAgainAfterwards() =
        check { d ->
            stored()
            val vault = model(d)
            val open = OpenScanner()
            assertFalse(vault.state.value.busy)
            vault.addPages(open)
            advanceUntilIdle()
            assertTrue(vault.state.value.busy)

            open.finish(PageScan.Cancelled)
            advanceUntilIdle()
            assertFalse(vault.state.value.busy)
            val again = scanner(PageScan.Cancelled)
            vault.addPages(again)
            advanceUntilIdle()
            assertEquals(1, again.limits.size, "a finished scan lets the next one start")
        }

    @Test
    fun leavingTheScreenDuringAScanStopsItAndStoresNothingMore() =
        check { d ->
            stored()
            val vault = model(d)
            val open = OpenScanner()
            vault.addPages(open)
            advanceUntilIdle()

            vault.close()
            open.finish(pages(1))
            advanceUntilIdle()

            assertEquals(emptyList(), repo.pages("order-1"))
            assertEquals(emptyList(), notices)
            assertEquals(0, changes)
        }

    @Test
    fun aScanAfterTheOrderWasDeletedIsIgnored() =
        check { d ->
            stored()
            val vault = model(d)
            vault.delete {}
            advanceUntilIdle()
            val scan = scanner(pages(1))
            vault.addPages(scan)
            advanceUntilIdle()
            assertEquals(emptyList(), scan.limits)
        }
}
