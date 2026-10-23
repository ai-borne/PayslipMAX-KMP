package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import kotlin.io.encoding.Base64

/** The drafts and orders that go into an archive's metadata document. */
class Part2Export(
    val drafts: List<Part2DraftBackup>,
    val orders: List<Part2OrderBackup>,
)

/**
 * What a restore will write, re-encrypted for this device, and how many rows it left out (unreadable, oversized, surplus or orphaned).
 * [pages] is lazy: each page is re-encrypted as the transaction inserts it, so the vault is never in memory twice.
 */
class Part2Restore(
    val drafts: List<Part2DraftEntity>,
    val orders: List<Part2OrderEntity>,
    val pages: Sequence<Part2PageEntity>,
    val skipped: Int,
)

/** An archive whose structure is damaged (cut short, a count that does not match, lengths that lie). Fixed message: no path, no content. */
class BackupDamagedException : Exception("This backup file is damaged and cannot be restored.")

/**
 * How the officer's drafts, vault orders and scanned pages move in and out of a `.pcda` archive (backup v5), following `GuideNoteBackup`:
 * a row that cannot be read is skipped and everything else still restores; on MERGE the newer `updatedAt` wins for the same id (a tie goes
 * to the backup); a page is immutable, so one the device already holds is left alone. Drafts and orders ride in the archive's JSON
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
    private val openPage: (Part2PageEntity, String) -> ByteArray = { page, key -> page.openPage(key) },
) {
    /** Device drafts and orders re-encrypted under [password]; a row this device cannot read is left out, like an unreadable payslip. */
    suspend fun export(password: String): Part2Export {
        val key = deviceKey()
        val drafts = dao.allDrafts().mapNotNull { row -> runCatching { Part2DraftBackup(row.id, Base64.encode(row.toDraft(key).toEntity(password).ciphertext)) }.getOrNull() }
        val orders = dao.allOrders().mapNotNull { row -> runCatching { Part2OrderBackup(row.id, Base64.encode(row.toOrder(key).toEntity(password).ciphertext)) }.getOrNull() }
        return Part2Export(drafts, orders)
    }

    /**
     * The finished archive when the vault has pages ([meta] is the encrypted metadata document), or null when it has none. The archive is
     * written into one buffer of its final size, one page at a time: a page's row, its plaintext and its new ciphertext are all that is
     * ever held besides the output.
     */
    suspend fun archiveWithPages(
        meta: ByteArray,
        password: String,
    ): ByteArray? {
        val refs = dao.allPageRefs()
        if (refs.isEmpty()) return null
        val key = deviceKey()
        val writer = PageArchiveWriter(PageArchive.capacity(meta.size, refs, dao.vaultBytes(), refs.size))
        writer.begin(meta)
        var written = 0
        for (ref in refs) {
            val row = dao.page(ref.id) ?: continue
            val jpeg = runCatching { row.openPage(key) }.getOrNull() ?: continue
            try {
                writer.add(ref.id, ref.orderId, ref.position, sealPage(ref.id, ref.orderId, ref.position, jpeg, password).ciphertext)
                written++
            } finally {
                jpeg.fill(0)
            }
        }
        return if (written == 0) null else writer.finish(written, password)
    }

    /** [archive] is the whole file; [backup] is its decoded metadata document. A damaged page archive throws [BackupDamagedException]. */
    suspend fun restore(
        backup: PortableBackup,
        archive: ByteArray,
        password: String,
        mode: RestoreMode,
    ): Part2Restore {
        val key = deviceKey()
        val merge = mode == RestoreMode.MERGE
        val drafts = restoreRows(draftKind, backup.part2Drafts, if (merge) dao.allDrafts() else emptyList(), password, key, merge, decodeText)
        val orders = restoreRows(orderKind, backup.part2Orders, if (merge) dao.allOrders() else emptyList(), password, key, merge, decodeText)
        val pages = restorePages(dao, archive, orders.kept + orders.heldIds, password, key, merge, vaultBytesMax, openPage)
        return Part2Restore(drafts.rows, orders.rows, pages.pages, drafts.skipped + orders.skipped + pages.skipped)
    }
}
