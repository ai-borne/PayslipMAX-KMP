package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakePart2Dao
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * WHY: what only the Room repository can get wrong: rows that cannot be read are counted but never shown or deleted, what reaches
 * the table is ciphertext, and the caps it hands the DAO are Part2Limits' (not a copy that could drift).
 */
class RoomPart2RepositoryTest {
    private val key = "device-key-for-tests"
    private val dao = FakePart2Dao()
    private val repo = RoomPart2Repository(dao, key = { key }, newId = { "page-x" }, dispatcher = Dispatchers.Unconfined)

    @Test
    fun theTablesHoldCiphertextOnly() =
        runTest {
            repo.saveDraft(SyntheticPart2Drafts.fullDraft())
            repo.saveOrder(SyntheticPart2Drafts.order())
            val jpeg = SyntheticPart2Drafts.jpeg(3000)
            repo.addPage("order-1", jpeg)

            val stored = dao.allDrafts().single().ciphertext + dao.allOrders().single().ciphertext
            for (secret in listOf(SyntheticPart2Drafts.MARKER_TEXT, SyntheticPart2Drafts.MARKER_ORDER_NO, "SYNCODE")) {
                assertFalse(stored.decodeToString().contains(secret), "$secret must not be readable")
            }
            val pageRow = dao.page("page-x")!!
            assertFalse(pageRow.ciphertext.toList().windowed(32).any { it == jpeg.copyOfRange(200, 232).toList() })
            assertContentEquals(jpeg, pageRow.openPage(key))
        }

    @Test
    fun unreadableRowsAreCountedKeptAndNeverShown() =
        runTest {
            repo.saveDraft(SyntheticPart2Drafts.fullDraft("good"))
            dao.upsertDraft(Part2DraftEntity("bad-garbage", byteArrayOf(1, 2, 3)))
            dao.upsertDraft(SyntheticPart2Drafts.fullDraft("bad-key").toEntity("a key this device no longer has"))

            val stored = repo.observeDrafts().first()

            assertEquals(listOf("good"), stored.items.map { it.id })
            assertEquals(2, stored.unreadable)
            assertNull(repo.draft("bad-garbage"))
            assertEquals(3, dao.draftCount(), "an unreadable row is never deleted behind the officer's back")
        }

    @Test
    fun unreadableOrdersAreCountedToo() =
        runTest {
            dao.upsertOrder(Part2OrderEntity("o", byteArrayOf(9)))

            val stored = repo.observeOrders().first()

            assertEquals(0, stored.items.size)
            assertEquals(1, stored.unreadable)
        }

    @Test
    fun anUnreadablePageReadsAsNullNotACrash() =
        runTest {
            dao.upsertPage(Part2PageEntity("p", "o", 0, byteArrayOf(1, 2)))

            assertNull(repo.readPage("p") { it })
        }

    @Test
    fun itHandsTheDaoTheCapsOfPart2LimitsNotACopy() =
        runTest {
            val seen = mutableListOf<Any>()
            val spy =
                object : Part2Dao by dao {
                    override suspend fun saveDraftWithin(
                        row: Part2DraftEntity,
                        max: Int,
                    ): Part2WriteResult = dao.saveDraftWithin(row, max).also { seen += max }

                    override suspend fun saveOrderWithin(
                        row: Part2OrderEntity,
                        max: Int,
                    ): Part2WriteResult = dao.saveOrderWithin(row, max).also { seen += max }

                    override suspend fun addPageWithin(
                        row: Part2PageEntity,
                        perOrderMax: Int,
                        vaultMax: Long,
                    ): Part2WriteResult =
                        dao.addPageWithin(row, perOrderMax, vaultMax).also {
                            seen += perOrderMax
                            seen += vaultMax
                        }
                }
            val real = RoomPart2Repository(spy, key = { key }, newId = { "p" }, dispatcher = Dispatchers.Unconfined)

            real.saveDraft(SyntheticPart2Drafts.emptyDraft())
            real.saveOrder(SyntheticPart2Drafts.order())
            real.addPage("order-1", SyntheticPart2Drafts.jpeg(100))

            assertEquals<List<Any>>(listOf(Part2Limits.DRAFTS_MAX, Part2Limits.ORDERS_MAX, Part2Limits.PAGES_PER_ORDER_MAX, Part2Limits.VAULT_BYTES_MAX), seen)
        }

    @Test
    fun aPageIsSealedWithThePageIdItWasGivenAndTheOrderItBelongsTo() =
        runTest {
            repo.saveOrder(SyntheticPart2Drafts.order())
            repo.addPage("order-1", SyntheticPart2Drafts.jpeg(100))

            val row = dao.page("page-x")!!
            assertEquals("order-1", row.orderId)
            assertContentEquals(SyntheticPart2Drafts.jpeg(100), CryptoHelper.decrypt(row.ciphertext, key).getOrThrow().copyOfRange(3 + "page-x".length + "order-1".length, 3 + "page-x".length + "order-1".length + 100))
        }
}
