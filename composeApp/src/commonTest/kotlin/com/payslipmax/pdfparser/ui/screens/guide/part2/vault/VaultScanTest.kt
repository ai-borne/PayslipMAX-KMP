package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.data.StoredPart2
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: scanning is the one way an order enters the vault. A scan that saved nothing must leave no empty order behind, a partly saved one keeps
 * what it got and says what it lost, a second tap while a scan is open does nothing, and a scan in flight owns its order and the scanner's pages.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultScanTest : VaultListTestBase() {
    @Test
    fun aNewOrderScannedFromADraftIsSavedLinkedToItAndOpened() =
        check { d ->
            val vault = model(d)
            val scan = scanner(pages(2))
            var opened: String? = null

            vault.addOrder("draft-1", scan) { opened = it }
            advanceUntilIdle()

            assertEquals("order-1", opened)
            assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX), scan.limits, "a new order has room for ten pages")
            val order = assertNotNull(repo.order("order-1"))
            assertEquals("draft-1", order.draftId)
            assertNull(order.orderNo)
            assertEquals(5_000L, order.createdAt)
            assertEquals(2, repo.pages("order-1").size)
            assertEquals(Part2VaultStrings.pagesSaved(2), vault.notice.value)
            assertEquals("2 pages · added 01/01/1970", vault.state.value.rows.single().line)
        }

    @Test
    fun anOrderScannedFromTheVaultHasNoDraft() =
        check { d ->
            val vault = model(d)
            vault.addOrder(null, scanner(pages(1))) {}
            advanceUntilIdle()
            assertNull(repo.order("order-1")?.draftId)
        }

    @Test
    fun aCancelledScanLeavesNoOrderBehindAndSaysNothing() =
        check { d ->
            val vault = model(d)
            var opened = false
            vault.addOrder("draft-1", scanner(PageScan.Cancelled)) { opened = true }
            advanceUntilIdle()

            assertFalse(opened)
            assertNull(repo.order("order-1"))
            assertNull(vault.notice.value)
            assertFalse(vault.state.value.hasAny)
        }

    @Test
    fun aScannerThatNeedsAConnectionLeavesNoOrderAndSaysSo() =
        check { d ->
            val vault = model(d)
            vault.addOrder(null, scanner(PageScan.Failed(ScanFailure.UNAVAILABLE))) {}
            advanceUntilIdle()

            assertNull(repo.order("order-1"))
            assertEquals(Part2VaultStrings.scanNeedsConnection, vault.notice.value)
        }

    @Test
    fun aScanWhosePagesAllFailLeavesNoOrderAndSaysHowManyWereLost() =
        check { d ->
            val vault = model(d)
            val unreadable = PageScan.Pages(listOf(FakeScannedPage(ByteArray(3), readable = false)))
            var opened = false
            vault.addOrder(null, scanner(unreadable)) { opened = true }
            advanceUntilIdle()

            assertFalse(opened)
            assertNull(repo.order("order-1"), "an order with no page is nothing to keep")
            assertEquals(Part2VaultStrings.pagesNotSaved(1), vault.notice.value)
        }

    @Test
    fun aFullVaultKeepsWhatWasSavedAndSaysTheRestWasRefused() =
        check { d ->
            val small = FakePart2Repository(vaultBytesMax = 450)
            val vault = model(d, small)
            var opened: String? = null
            vault.addOrder(null, scanner(pages(2))) { opened = it }
            advanceUntilIdle()

            assertEquals("order-1", opened)
            assertEquals(1, small.pages("order-1").size)
            assertEquals("${Part2VaultStrings.pagesSaved(1)} ${Part2VaultStrings.vaultFull}", vault.notice.value)
        }

    @Test
    fun anOrderThatCannotBeSavedNeverOpensTheScanner() =
        check { d ->
            repo.refuseOrders = Part2WriteResult.TOO_MANY
            val vault = model(d)
            val scan = scanner(pages(1))
            var opened = false
            vault.addOrder(null, scan) { opened = true }
            advanceUntilIdle()

            assertEquals(emptyList(), scan.limits, "the scanner is not opened when there is nowhere to keep the pages")
            assertFalse(opened)
            assertEquals(Part2VaultStrings.saveFailed, vault.notice.value)
        }

    @Test
    fun aSecondTapWhileAScanIsOpenIsIgnoredAndTheListIsBusy() =
        check { d ->
            val vault = model(d)
            val open = OpenScanner()
            vault.addOrder(null, open) {}
            advanceUntilIdle()
            assertTrue(vault.state.value.busy)

            vault.addOrder(null, open) {}
            advanceUntilIdle()
            assertEquals(1, open.scans)
            assertEquals(1, repo.orderSaves)

            open.finish(PageScan.Cancelled)
            advanceUntilIdle()
            assertFalse(vault.state.value.busy)
            vault.addOrder(null, scanner(PageScan.Cancelled)) {}
            advanceUntilIdle()
            assertEquals(2, repo.orderSaves, "the next tap works once the scan has ended")
        }

    @Test
    fun theNoticeStaysUntilDismissed() =
        check { d ->
            val vault = model(d)
            vault.addOrder(null, scanner(PageScan.Failed(ScanFailure.FAILED))) {}
            advanceUntilIdle()
            assertEquals(Part2VaultStrings.scanFailed, vault.notice.value)

            vault.dismissNotice()
            assertNull(vault.notice.value)
        }

    @Test
    fun aNewScanClearsTheOldNoticeBeforeItStarts() =
        check { d ->
            val vault = model(d)
            vault.addOrder(null, scanner(PageScan.Failed(ScanFailure.FAILED))) {}
            advanceUntilIdle()
            val open = OpenScanner()
            vault.addOrder(null, open) {}
            advanceUntilIdle()
            assertNull(vault.notice.value, "an old failure must not sit beside a scan in progress")
            open.finish(PageScan.Cancelled)
            advanceUntilIdle()
        }

    // ---- deleting, opening, leftovers ----

    @Test
    fun openingTheVaultDuringAScanLeavesTheOrderBeingScannedAlone() =
        check { d ->
            val vault = model(d)
            val open = OpenScanner()
            vault.addOrder(null, open) {}
            advanceUntilIdle()
            val other = OpenScanner()

            vault.opened(other)
            advanceUntilIdle()

            assertNotNull(repo.order("order-1"), "the order a scan is filling is not a ghost")
            assertEquals(0, other.leftoversDiscarded, "and the scanner's pages are not swept from under it")
            open.finish(pages(1))
            advanceUntilIdle()
            assertEquals(1, repo.pages("order-1").size)
        }

    @Test
    fun aNewOrdersRowCountsItsPagesOnceTheScanHasStoredThem() =
        check { d ->
            val vault = model(d)
            val open = OpenScanner()
            vault.addOrder(null, open) {}
            advanceUntilIdle()
            assertEquals(Part2VaultStrings.orderRowLine(0, "01/01/1970"), vault.state.value.rows.single().line, "while the scanner is open the order has no page yet")

            open.finish(pages(2))
            advanceUntilIdle()

            assertEquals(Part2VaultStrings.orderRowLine(2, "01/01/1970"), vault.state.value.rows.single().line, "the row is refreshed when the pages are in, not left at the count it was saved with")
        }

    @Test
    fun aScanThatStartsWhileTheVaultIsBeingTidiedKeepsItsOrder() =
        check { d ->
            val slow =
                object : Part2Repository by repo {
                    override fun observeOrders(): Flow<StoredPart2<VaultOrder>> =
                        flow {
                            delay(50)
                            emitAll(repo.observeOrders())
                        }
                }
            val vault = model(d, slow)
            val open = OpenScanner()

            vault.opened(scanner())
            advanceTimeBy(10)
            vault.addOrder(null, open) {}
            advanceTimeBy(20)
            advanceUntilIdle()

            assertNotNull(repo.order("order-1"), "the tidy-up read the list after the shell was saved, and still left it alone")
            open.finish(pages(1))
            advanceUntilIdle()
            assertEquals(1, repo.pages("order-1").size)
        }
}
