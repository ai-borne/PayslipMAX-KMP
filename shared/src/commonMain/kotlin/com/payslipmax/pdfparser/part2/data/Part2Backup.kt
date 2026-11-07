package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.MemoryBackupSource
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import kotlin.io.encoding.Base64

/** The entries and orders that go into an archive's metadata document. */
class Part2Export(
    val entries: List<Part2RowBackup>,
    val orders: List<Part2RowBackup>,
)

/**
 * What a restore will write, re-encrypted for this device, and the rows it left out per kind (unreadable, oversized, surplus, orphaned or a
 * draft from the frozen branch, counted as an entry: entries replaced the drafts). [pages] is lazy: each page is re-encrypted as the transaction inserts it, so the vault is never in memory
 * twice; [pageCount] is how many it will yield (each one already checked).
 */
class Part2Restore(
    val entries: List<Part2EntryEntity>,
    val orders: List<Part2OrderEntity>,
    val pages: Sequence<Part2PageEntity>,
    val skipped: SkippedRows,
    val pageCount: Int,
)

/**
 * How the officer's Bank entries, vault orders and scanned pages move in and out of a `.pcda` archive (backup v5), following `GuideNoteBackup`:
 * a row that cannot be read is skipped and everything else still restores; on MERGE the newer `updatedAt` wins for the same id (a tie goes
 * to the backup); a page is immutable, so one the device already holds is left alone. Entries and orders ride in the archive's JSON
 * document; pages are binary records after it (see [PageArchive]), so a full vault costs about its own size in memory, not thirteen
 * times. A restored file is untrusted input: every row's id, size and count is checked against `Part2Limits` BEFORE it is decoded or
 * decrypted, and what fails is skipped and counted. Nothing here logs and no exception carries a row.
 */
class Part2Backup(
    private val dao: Part2Dao,
    private val deviceKey: () -> String = { CryptoHelper.getDatabaseSecretKey() },
    private val vaultBytesMax: Long = Part2Limits.VAULT_BYTES_MAX,
    // Seams so a test can prove an over-limit row is never decoded or decrypted.
    private val decodeText: (String) -> ByteArray = { Base64.decode(it) },
    private val openPage: (Part2PageEntity, SealKey) -> ByteArray = { page, key -> page.openPage(key) },
) {
    /** How many entries and vault orders this device holds (pages belong to an order, so they add nothing). */
    suspend fun itemCount(): Int = dao.entryCount() + dao.orderCount()

    /** The bytes the vault's pages take on the device: a backup copies them, so this is most of the export file's size. */
    suspend fun vaultBytes(): Long = dao.vaultBytes()

    /** Device entries and orders sealed with the backup's [backupKey]; a row this device cannot read is left out, like an unreadable payslip. */
    suspend fun export(backupKey: SealKey): Part2Export {
        val key = deviceKey()
        val entries = dao.allEntries().mapNotNull { row -> runCatching { Part2RowBackup(row.id, Base64.encode(row.toEntry(key).toEntity(backupKey).ciphertext)) }.getOrNull() }
        val orders = dao.allOrders().mapNotNull { row -> runCatching { Part2RowBackup(row.id, Base64.encode(row.toOrder(key).toEntity(backupKey).ciphertext)) }.getOrNull() }
        return Part2Export(entries, orders)
    }

    /**
     * Writes every page this device can read into [writer], sealed with the backup's [backupKey], and returns how many it wrote (the
     * trailer's count). One page at a time: a page's row, its plaintext and its new ciphertext are all that is ever held, so the vault can be
     * any size the ceiling allows. A page this device cannot read is left out, like an unreadable payslip, and so is a page whose order is
     * not in [orderIds] (the orders this backup carries): a restore could only skip it, and a fresh backup must read back with nothing skipped.
     */
    suspend fun writePages(
        writer: PageArchiveWriter,
        backupKey: SealKey,
        orderIds: Set<String>,
        onPage: (written: Int, total: Int) -> Unit = { _, _ -> },
    ): Int {
        val key = deviceKey()
        var written = 0
        val refs = dao.allPageRefs().filter { it.orderId in orderIds }
        for (ref in refs) {
            val row = dao.page(ref.id) ?: continue
            val jpeg = runCatching { row.openPage(key) }.getOrNull() ?: continue
            try {
                writer.add(ref.id, ref.orderId, ref.position, sealPage(ref.id, ref.orderId, ref.position, jpeg, backupKey).ciphertext)
                written++
                onPage(written, refs.size)
            } finally {
                jpeg.fill(0)
            }
        }
        return written
    }

    /** [archive] is the whole file in memory (small files and tests); see the [BackupSource] form. */
    suspend fun restore(
        backup: PortableBackup,
        archive: ByteArray,
        backupKey: SealKey,
        mode: RestoreMode,
    ): Part2Restore = restore(backup, MemoryBackupSource(archive), backupKey, mode)

    /** [source] is the whole file, read by range; [backup] is its decoded metadata document. A damaged page archive throws `BackupDamagedException`. */
    suspend fun restore(
        backup: PortableBackup,
        source: BackupSource,
        backupKey: SealKey,
        mode: RestoreMode,
    ): Part2Restore {
        val key = deviceKey()
        val merge = mode == RestoreMode.MERGE
        val entries = restoreRows(entryKind, backup.part2Entries, if (merge) dao.allEntries() else emptyList(), backupKey, key, merge, decodeText)
        val orders = restoreRows(orderKind, backup.part2Orders, if (merge) dao.allOrders() else emptyList(), backupKey, key, merge, decodeText)
        val pages = restorePages(dao, source, orders.kept + orders.heldIds, backupKey, key, merge, vaultBytesMax, openPage)
        val skipped = SkippedRows(part2Entries = entries.skipped + backup.part2Drafts.size, part2Orders = orders.skipped, part2Pages = pages.skipped)
        return Part2Restore(entries.rows, orders.rows, pages.pages, skipped, pages.count)
    }
}
