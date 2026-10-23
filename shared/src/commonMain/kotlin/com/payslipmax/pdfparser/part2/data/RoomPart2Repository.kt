package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [Part2Repository] over the encrypted Part II tables. Each save is one Room transaction, so a process kill leaves the old item or the
 * new one, never half. Encryption and decryption run on [dispatcher], off the main thread. Every cap handed to the DAO is
 * `Part2Limits`' ([vaultBytesMax] defaults to it and exists so a test can use a small vault). Nothing here logs: no failure carries
 * what was in the item.
 */
class RoomPart2Repository
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val dao: Part2Dao,
        private val key: () -> String = { CryptoHelper.getDatabaseSecretKey() },
        private val newId: () -> String = { Uuid.random().toString() },
        private val vaultBytesMax: Long = Part2Limits.VAULT_BYTES_MAX,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : Part2Repository {
        // A page's position is read and then written; this makes the two one step per repository, so two pages never share a position.
        private val pageLock = Mutex()

        override fun observeDrafts(): Flow<StoredPart2<Part2Draft>> =
            dao.observeDrafts().map { rows -> read(rows) { it.toDraft(key()) }.let { (items, bad) -> StoredPart2(items.sortedByDescending { it.updatedAt }, bad) } }.flowOn(dispatcher)

        override suspend fun draft(id: String): Part2Draft? =
            withContext(dispatcher) {
                dao.draft(id)?.let { row ->
                    try {
                        row.toDraft(key())
                    } catch (e: Part2UnreadableException) {
                        null
                    }
                }
            }

        override suspend fun saveDraft(draft: Part2Draft): Part2WriteResult =
            withContext(dispatcher) {
                Part2Bounds.check(draft) ?: try {
                    dao.saveDraftWithin(draft.toEntity(key()), Part2Limits.DRAFTS_MAX)
                } catch (e: Part2TooLargeException) {
                    Part2WriteResult.TOO_LARGE
                }
            }

        override suspend fun deleteDraft(id: String) = withContext(dispatcher) { dao.deleteDraft(id) }

        override fun observeOrders(): Flow<StoredPart2<VaultOrder>> =
            dao.observeOrders().map { rows -> read(rows) { it.toOrder(key()) }.let { (items, bad) -> StoredPart2(items.sortedByDescending { it.updatedAt }, bad) } }.flowOn(dispatcher)

        override suspend fun saveOrder(order: VaultOrder): Part2WriteResult =
            withContext(dispatcher) {
                Part2Bounds.check(order) ?: try {
                    dao.saveOrderWithin(order.toEntity(key()), Part2Limits.ORDERS_MAX)
                } catch (e: Part2TooLargeException) {
                    Part2WriteResult.TOO_LARGE
                }
            }

        override suspend fun deleteOrder(id: String) = withContext(dispatcher) { dao.deleteOrderWithPages(id) }

        override suspend fun pages(orderId: String): List<VaultPageRef> = withContext(dispatcher) { dao.pageRefs(orderId) }

        override suspend fun addPage(
            orderId: String,
            jpeg: ByteArray,
        ): AddedPage =
            withContext(dispatcher) {
                when {
                    jpeg.isEmpty() || jpeg.size > Part2Limits.PAGE_MAX_BYTES -> AddedPage(Part2WriteResult.TOO_LARGE)
                    !Part2Limits.isPlainId(orderId) -> AddedPage(Part2WriteResult.NO_SUCH_ORDER)
                    else ->
                        pageLock.withLock {
                            val id = newId()
                            val row = sealPage(id, orderId, dao.nextPosition(orderId), jpeg, key())
                            val result = dao.addPageWithin(row, Part2Limits.PAGES_PER_ORDER_MAX, vaultBytesMax)
                            AddedPage(result, id.takeIf { result == Part2WriteResult.SAVED })
                        }
                }
            }

        override suspend fun <T> readPage(
            pageId: String,
            use: (ByteArray) -> T,
        ): T? =
            withContext(dispatcher) {
                val row = dao.page(pageId) ?: return@withContext null
                val jpeg =
                    try {
                        row.openPage(key())
                    } catch (e: Part2UnreadableException) {
                        return@withContext null
                    }
                try {
                    use(jpeg)
                } finally {
                    jpeg.fill(0)
                }
            }

        override suspend fun deletePage(pageId: String) = withContext(dispatcher) { dao.deletePage(pageId) }

        override suspend fun vaultBytes(): Long = withContext(dispatcher) { dao.vaultBytes() }

        private fun <R, T> read(
            rows: List<R>,
            open: (R) -> T,
        ): Pair<List<T>, Int> {
            val items = mutableListOf<T>()
            var unreadable = 0
            for (row in rows) {
                try {
                    items += open(row)
                } catch (e: Part2UnreadableException) {
                    unreadable++
                }
            }
            return items to unreadable
        }
    }
