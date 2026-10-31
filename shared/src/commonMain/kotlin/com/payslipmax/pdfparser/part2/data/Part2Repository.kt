package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import kotlinx.coroutines.flow.Flow

/**
 * What the device holds of one kind. [unreadable] counts rows that could not be decrypted (for example after the device key was
 * lost): they stay on disk, are never shown and never deleted behind the officer's back, so a screen can say "N items could not be read".
 * [toString] shows counts only.
 */
class StoredPart2<T>(
    val items: List<T>,
    val unreadable: Int = 0,
) {
    override fun toString(): String = "StoredPart2(${items.size}, unreadable=$unreadable)"
}

/** The outcome of adding a page: [pageId] is set only when [result] is [Part2WriteResult.SAVED]. */
class AddedPage(
    val result: Part2WriteResult,
    val pageId: String? = null,
)

/**
 * The officer's Orders Bank entries, vault orders and scanned pages. They stay on the device, encrypted, and leave it only inside the
 * officer's own `.pcda` backup. Every cap is `Part2Limits`'; an implementation cannot store an item that breaks them. Premium is not
 * checked here (a lapsed officer can still view, delete and back up): the screens decide who may create.
 */
interface Part2Repository {
    /** Every readable entry, newest edit first, emitting again after each change. */
    fun observeEntries(): Flow<StoredPart2<BankEntry>>

    /** The entry, or null when there is none or it cannot be read. */
    suspend fun entry(id: String): BankEntry?

    suspend fun saveEntry(entry: BankEntry): Part2WriteResult

    suspend fun deleteEntry(id: String)

    fun observeOrders(): Flow<StoredPart2<VaultOrder>>

    /** The order, or null when there is none or it cannot be read. */
    suspend fun order(id: String): VaultOrder?

    suspend fun saveOrder(order: VaultOrder): Part2WriteResult

    /** Deletes the order and all its pages in one step. */
    suspend fun deleteOrder(id: String)

    suspend fun pages(orderId: String): List<VaultPageRef>

    /** Adds a re-encoded JPEG page at the end of the order's pages. The bytes are not kept by the repository. */
    suspend fun addPage(
        orderId: String,
        jpeg: ByteArray,
    ): AddedPage

    /** Decrypts the page, hands it to [use], then overwrites it with zeros. The bytes must not be kept past [use]. Null when missing or unreadable. */
    suspend fun <T> readPage(
        pageId: String,
        use: (ByteArray) -> T,
    ): T?

    suspend fun deletePage(pageId: String)

    /** Stored page ciphertext in bytes. */
    suspend fun vaultBytes(): Long
}
