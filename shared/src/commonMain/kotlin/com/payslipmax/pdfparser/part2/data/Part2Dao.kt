package com.payslipmax.pdfparser.part2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import kotlinx.coroutines.flow.Flow

/** How a write to the Part II tables ended. Every refusal is a cap from `Part2Limits`; none says what was in the item. */
enum class Part2WriteResult { SAVED, REJECTED, TOO_LARGE, TOO_MANY, VAULT_FULL, NO_SUCH_ORDER }

/**
 * The Part II tables. The `@Transaction` methods hold the cap checks, so a count or size can never be passed by two writes racing.
 * Restore writes and "delete all" go through `PayslipDao` instead (they must share one transaction with everything else the user
 * created); this DAO is what the repository and the backup export read.
 */
@Dao
interface Part2Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(row: Part2DraftEntity)

    @Query("SELECT * FROM part2_drafts WHERE id = :id")
    suspend fun draft(id: String): Part2DraftEntity?

    @Query("SELECT * FROM part2_drafts")
    fun observeDrafts(): Flow<List<Part2DraftEntity>>

    @Query("SELECT * FROM part2_drafts")
    suspend fun allDrafts(): List<Part2DraftEntity>

    @Query("SELECT COUNT(*) FROM part2_drafts")
    suspend fun draftCount(): Int

    @Query("DELETE FROM part2_drafts WHERE id = :id")
    suspend fun deleteDraft(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(row: Part2OrderEntity)

    @Query("SELECT * FROM part2_orders WHERE id = :id")
    suspend fun order(id: String): Part2OrderEntity?

    @Query("SELECT * FROM part2_orders")
    fun observeOrders(): Flow<List<Part2OrderEntity>>

    @Query("SELECT * FROM part2_orders")
    suspend fun allOrders(): List<Part2OrderEntity>

    @Query("SELECT COUNT(*) FROM part2_orders")
    suspend fun orderCount(): Int

    @Query("DELETE FROM part2_orders WHERE id = :id")
    suspend fun deleteOrder(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(row: Part2PageEntity)

    @Query("SELECT * FROM part2_pages WHERE id = :id")
    suspend fun page(id: String): Part2PageEntity?

    @Query("SELECT id, orderId, position FROM part2_pages WHERE orderId = :orderId ORDER BY position")
    suspend fun pageRefs(orderId: String): List<VaultPageRef>

    @Query("SELECT id, orderId, position FROM part2_pages ORDER BY orderId, position")
    suspend fun allPageRefs(): List<VaultPageRef>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM part2_pages WHERE orderId = :orderId")
    suspend fun nextPosition(orderId: String): Int

    /** The vault's size: stored page ciphertext in bytes. Settings warns above `Part2Limits.VAULT_WARN_BYTES`. */
    @Query("SELECT COALESCE(SUM(LENGTH(ciphertext)), 0) FROM part2_pages")
    suspend fun vaultBytes(): Long

    @Query("DELETE FROM part2_pages WHERE id = :id")
    suspend fun deletePage(id: String)

    @Query("DELETE FROM part2_pages WHERE orderId = :orderId")
    suspend fun deletePagesOf(orderId: String)

    /** Saves [row] unless it is a new draft and the device already holds [max]. */
    @Transaction
    suspend fun saveDraftWithin(
        row: Part2DraftEntity,
        max: Int,
    ): Part2WriteResult {
        if (draft(row.id) == null && draftCount() >= max) return Part2WriteResult.TOO_MANY
        upsertDraft(row)
        return Part2WriteResult.SAVED
    }

    @Transaction
    suspend fun saveOrderWithin(
        row: Part2OrderEntity,
        max: Int,
    ): Part2WriteResult {
        if (order(row.id) == null && orderCount() >= max) return Part2WriteResult.TOO_MANY
        upsertOrder(row)
        return Part2WriteResult.SAVED
    }

    /** Adds a page unless its order is missing, the order already holds [perOrderMax] pages, or the vault would pass [vaultMax] bytes. */
    @Transaction
    suspend fun addPageWithin(
        row: Part2PageEntity,
        perOrderMax: Int,
        vaultMax: Long,
    ): Part2WriteResult {
        if (order(row.orderId) == null) return Part2WriteResult.NO_SUCH_ORDER
        if (pageRefs(row.orderId).size >= perOrderMax) return Part2WriteResult.TOO_MANY
        if (vaultBytes() + row.ciphertext.size > vaultMax) return Part2WriteResult.VAULT_FULL
        upsertPage(row)
        return Part2WriteResult.SAVED
    }

    /** Deleting an order deletes its pages. Deleting a draft does not touch its orders (no foreign key: a REPLACE on a parent would cascade). */
    @Transaction
    suspend fun deleteOrderWithPages(id: String) {
        deletePagesOf(id)
        deleteOrder(id)
    }
}
