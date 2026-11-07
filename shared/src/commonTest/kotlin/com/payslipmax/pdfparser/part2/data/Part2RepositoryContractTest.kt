package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakePart2Dao
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the Room repository and the fake the ViewModels are tested with must obey the same rules, or a ViewModel test could pass on a
 * behaviour production does not have. Both run this one suite. The rules are the officer's: autosave never loses a entry, deleting a
 * entry keeps the orders scanned from it, deleting an order takes its pages, and every cap is Part2Limits'.
 */
abstract class Part2RepositoryContractTest {
    abstract fun repository(vaultBytesMax: Long = Part2Limits.VAULT_BYTES_MAX): Part2Repository

    private suspend fun Part2Repository.withOrder(id: String = "order-1"): Part2Repository {
        assertEquals(Part2WriteResult.SAVED, saveOrder(SyntheticBank.order(id = id)))
        return this
    }

    @Test
    fun aSavedEntryIsReadBackAndObservedNewestEditFirst() =
        runTest {
            val repo = repository()
            repo.saveEntry(SyntheticBank.entry("d-old", updatedAt = 10))
            repo.saveEntry(SyntheticBank.entry("d-new", updatedAt = 20))

            assertEquals(SyntheticBank.entry("d-new", updatedAt = 20), repo.entry("d-new"))
            assertEquals(listOf("d-new", "d-old"), repo.observeEntries().first().items.map { it.id })
            assertNull(repo.entry("missing"))
        }

    @Test
    fun savingTheSameIdAgainReplacesTheEntryAsAutosaveDoes() =
        runTest {
            val repo = repository()
            repo.saveEntry(SyntheticBank.entry("d", updatedAt = 1))
            repo.saveEntry(SyntheticBank.entry("d", updatedAt = 2))

            assertEquals(1, repo.observeEntries().first().items.size)
            assertEquals(2L, repo.entry("d")?.updatedAt)
        }

    @Test
    fun aEntryThatBreaksTheBoundsIsRefusedAndNothingIsStored() =
        runTest {
            val repo = repository()
            val tooLong = SyntheticBank.entry().copy(id = "a/b")

            assertEquals(Part2WriteResult.REJECTED, repo.saveEntry(tooLong))
            assertTrue(repo.observeEntries().first().items.isEmpty())
        }

    @Test
    fun deletingAEntryKeepsTheOrdersScannedFromIt() =
        runTest {
            val repo = repository().withOrder()
            repo.saveEntry(SyntheticBank.entry("entry-1"))

            repo.deleteEntry("entry-1")

            assertNull(repo.entry("entry-1"))
            assertEquals(listOf("order-1"), repo.observeOrders().first().items.map { it.id })
        }

    @Test
    fun deletingAnOrderDeletesItsPagesAndOnlyItsPages() =
        runTest {
            val repo = repository().withOrder("order-1").withOrder("order-2")
            val doomed = repo.addPage("order-1", SyntheticBank.jpeg(1000)).pageId
            val kept = repo.addPage("order-2", SyntheticBank.jpeg(1000)).pageId

            repo.deleteOrder("order-1")

            assertTrue(repo.pages("order-1").isEmpty())
            assertNull(repo.readPage(doomed!!) { it.size })
            assertEquals(1000, repo.readPage(kept!!) { it.size })
            assertEquals(listOf("order-2"), repo.observeOrders().first().items.map { it.id })
        }

    @Test
    fun deletingAnOrderLeavesTheEntriesThatNamedItBecauseTheirStatusIsDerived() =
        runTest {
            val repo = repository().withOrder()
            repo.saveEntry(SyntheticBank.entry("e-1", orderId = "order-1"))

            repo.deleteOrder("order-1")

            assertEquals("order-1", repo.entry("e-1")?.orderId, "no cascade write: BankStatus reads the dangling id as waiting")
            assertNull(repo.order("order-1"))
        }

    @Test
    fun anOrderIsReadBackByIdAndAMissingOneIsNull() =
        runTest {
            val repo = repository()
            repo.saveOrder(SyntheticBank.order("o-1", orderNo = "N/1"))
            repo.saveOrder(SyntheticBank.order("o-2", orderNo = "N/2"))

            assertEquals(SyntheticBank.order("o-2", orderNo = "N/2"), repo.order("o-2"))
            assertNull(repo.order("missing"))
            repo.deleteOrder("o-2")
            assertNull(repo.order("o-2"))
            assertNotNull(repo.order("o-1"))
        }

    @Test
    fun pagesKeepTheirOrderAndANewPageNeverTakesAPositionAlreadyUsed() =
        runTest {
            val repo = repository().withOrder()
            val ids = (1..3).map { repo.addPage("order-1", SyntheticBank.jpeg(500, it)).pageId!! }
            repo.deletePage(ids[1])
            val added = repo.addPage("order-1", SyntheticBank.jpeg(500, 9)).pageId!!

            val positions = repo.pages("order-1")
            assertEquals(listOf(ids[0], ids[2], added), positions.map { it.id })
            assertEquals(positions.size, positions.map { it.position }.toSet().size, "no two pages share a position")
            assertEquals(positions.map { it.position }.sorted(), positions.map { it.position })
        }

    @Test
    fun aPageOverFiveHundredKilobytesOrEmptyIsRefused() =
        runTest {
            val repo = repository().withOrder()

            assertEquals(Part2WriteResult.SAVED, repo.addPage("order-1", SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES)).result)
            assertEquals(Part2WriteResult.TOO_LARGE, repo.addPage("order-1", SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES + 1)).result)
            assertEquals(Part2WriteResult.TOO_LARGE, repo.addPage("order-1", ByteArray(0)).result)
            assertEquals(1, repo.pages("order-1").size)
        }

    @Test
    fun anOrderTakesTenPagesAndRefusesTheEleventh() =
        runTest {
            val repo = repository().withOrder()
            repeat(Part2Limits.PAGES_PER_ORDER_MAX) { assertEquals(Part2WriteResult.SAVED, repo.addPage("order-1", SyntheticBank.jpeg(300, it)).result) }

            val eleventh = repo.addPage("order-1", SyntheticBank.jpeg(300))

            assertEquals(Part2WriteResult.TOO_MANY, eleventh.result)
            assertNull(eleventh.pageId)
            assertEquals(Part2Limits.PAGES_PER_ORDER_MAX, repo.pages("order-1").size)
        }

    @Test
    fun aPageForAnOrderThatDoesNotExistIsRefused() =
        runTest {
            assertEquals(Part2WriteResult.NO_SUCH_ORDER, repository().addPage("nope", SyntheticBank.jpeg(300)).result)
        }

    @Test
    fun theVaultRefusesAPageThatWouldPassItsCeilingAndShrinksWhenPagesGo() =
        runTest {
            val repo = repository(vaultBytesMax = 5000).withOrder()
            val first = repo.addPage("order-1", SyntheticBank.jpeg(2000, 1)).pageId!!
            assertEquals(Part2WriteResult.SAVED, repo.addPage("order-1", SyntheticBank.jpeg(2000, 2)).result)

            assertEquals(Part2WriteResult.VAULT_FULL, repo.addPage("order-1", SyntheticBank.jpeg(2000, 3)).result)
            assertTrue(repo.vaultBytes() >= 4000)
            repo.deletePage(first)
            assertEquals(Part2WriteResult.SAVED, repo.addPage("order-1", SyntheticBank.jpeg(2000, 4)).result)
            repo.deleteOrder("order-1")
            assertEquals(0L, repo.vaultBytes())
        }

    @Test
    fun aPageIsHandedOverOnceAndWipedWhenTheCallbackReturns() =
        runTest {
            val repo = repository().withOrder()
            val jpeg = SyntheticBank.jpeg(800, 5)
            val id = repo.addPage("order-1", jpeg).pageId!!
            var seen: ByteArray? = null

            val sameBytes =
                repo.readPage(id) { bytes ->
                    seen = bytes
                    bytes.contentEquals(jpeg)
                }

            assertEquals(true, sameBytes)
            assertTrue(assertNotNull(seen).all { it == 0.toByte() }, "no plaintext page outlives the call that used it")
            assertNull(repo.readPage("missing") { it })
        }
}

class FakePart2RepositoryContractTest : Part2RepositoryContractTest() {
    override fun repository(vaultBytesMax: Long): Part2Repository = FakePart2Repository(vaultBytesMax)
}

class RoomPart2RepositoryContractTest : Part2RepositoryContractTest() {
    override fun repository(vaultBytesMax: Long): Part2Repository {
        var next = 1
        return RoomPart2Repository(FakePart2Dao(), key = { "device-key-for-tests" }, newId = { "page-${next++}" }, vaultBytesMax = vaultBytesMax, dispatcher = Dispatchers.Unconfined)
    }
}
