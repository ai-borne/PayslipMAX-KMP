package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.testing.FakePart2Dao
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the count and size caps sit inside the DAO's own transactions, so two writes racing cannot both pass a check. These run the
 * interface's default bodies (the same code Room runs) on raw rows, with small maxima; the repository passes Part2Limits' values
 * (pinned in RoomPart2RepositoryTest).
 */
class Part2DaoCapsTest {
    private val dao = FakePart2Dao()

    private fun entry(id: String) = Part2EntryEntity(id, byteArrayOf(1))

    private fun order(id: String) = Part2OrderEntity(id, byteArrayOf(1))

    private fun page(
        id: String,
        orderId: String = "o1",
        size: Int = 10,
        position: Int = 0,
    ) = Part2PageEntity(id, orderId, position, ByteArray(size))

    @Test
    fun aNewEntryAtTheCeilingIsRefusedButAnExistingOneCanStillBeEdited() =
        runTest {
            assertEquals(Part2WriteResult.SAVED, dao.saveEntryWithin(entry("a"), 2))
            assertEquals(Part2WriteResult.SAVED, dao.saveEntryWithin(entry("b"), 2))

            assertEquals(Part2WriteResult.TOO_MANY, dao.saveEntryWithin(entry("c"), 2))
            assertEquals(Part2WriteResult.SAVED, dao.saveEntryWithin(entry("a"), 2), "autosave of a entry already held is not a new one")
            assertEquals(2, dao.entryCount())
            assertNull(dao.entry("c"))
        }

    @Test
    fun aNewOrderAtTheCeilingIsRefusedButAnExistingOneCanStillBeEdited() =
        runTest {
            assertEquals(Part2WriteResult.SAVED, dao.saveOrderWithin(order("a"), 1))

            assertEquals(Part2WriteResult.TOO_MANY, dao.saveOrderWithin(order("b"), 1))
            assertEquals(Part2WriteResult.SAVED, dao.saveOrderWithin(order("a"), 1))
            assertEquals(1, dao.orderCount())
        }

    @Test
    fun aPageNeedsItsOrderAndStopsAtThePerOrderAndVaultCeilings() =
        runTest {
            assertEquals(Part2WriteResult.NO_SUCH_ORDER, dao.addPageWithin(page("p0"), 2, 1000))
            dao.upsertOrder(order("o1"))

            assertEquals(Part2WriteResult.SAVED, dao.addPageWithin(page("p1"), 2, 1000))
            assertEquals(Part2WriteResult.SAVED, dao.addPageWithin(page("p2", position = 1), 2, 1000))
            assertEquals(Part2WriteResult.TOO_MANY, dao.addPageWithin(page("p3", position = 2), 2, 1000))
            assertEquals(Part2WriteResult.VAULT_FULL, dao.addPageWithin(page("p4", size = 981), 3, 1000))
            assertEquals(Part2WriteResult.SAVED, dao.addPageWithin(page("p5", size = 980, position = 2), 3, 1000))
            assertEquals(1000L, dao.vaultBytes())
        }

    @Test
    fun deletingAnOrderRemovesItsPagesInOneStepAndLeavesEntriesAndOtherOrdersAlone() =
        runTest {
            dao.upsertEntry(entry("d"))
            dao.upsertOrder(order("o1"))
            dao.upsertOrder(order("o2"))
            dao.upsertPage(page("p1", "o1"))
            dao.upsertPage(page("p2", "o2"))

            dao.deleteOrderWithPages("o1")

            assertNull(dao.order("o1"))
            assertNull(dao.page("p1"))
            assertEquals("p2", dao.page("p2")?.id)
            assertEquals("d", dao.entry("d")?.id)
        }
}
