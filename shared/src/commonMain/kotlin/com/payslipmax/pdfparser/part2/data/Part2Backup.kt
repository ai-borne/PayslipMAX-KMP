package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import kotlin.io.encoding.Base64

/** What goes into an archive. */
class Part2Export(
    val drafts: List<Part2DraftBackup>,
    val orders: List<Part2OrderBackup>,
    val pages: List<Part2PageBackup>,
)

/** What a restore will write, already re-encrypted for this device, and how many rows it left out (unreadable, oversized, surplus or orphaned). */
class Part2Restore(
    val drafts: List<Part2DraftEntity>,
    val orders: List<Part2OrderEntity>,
    val pages: List<Part2PageEntity>,
    val skipped: Int,
)

/**
 * How the officer's drafts, vault orders and scanned pages move in and out of a `.pcda` archive (backup v5), following
 * `GuideNoteBackup`: a row that cannot be read is skipped and everything else still restores; on MERGE the newer `updatedAt` wins for
 * the same id (a tie goes to the backup); a page is immutable, so one the device already holds is left alone.
 *
 * A restored file is untrusted input, and each row costs a key derivation and a decryption. So every row's id, text length and count
 * is checked against `Part2Limits` BEFORE it is decoded: a row over its size, rows past the count, a page past an order's ten, or
 * pages past the vault ceiling are skipped and counted, never decoded. (The archive's JSON itself is already in memory by then; that
 * is the existing archive format's limit, recorded in Phase 11.) Nothing here logs and no exception carries a row.
 */
class Part2Backup(
    private val dao: Part2Dao,
    private val deviceKey: () -> String = { CryptoHelper.getDatabaseSecretKey() },
    private val vaultBytesMax: Long = Part2Limits.VAULT_BYTES_MAX,
    // The only place a row's text is turned into bytes: a seam so a test can prove an over-limit row is never decoded.
    private val decodeText: (String) -> ByteArray = { Base64.decode(it) },
) {
    /** Device rows re-encrypted under [password]; a row this device cannot read is left out, like an unreadable payslip. */
    suspend fun export(password: String): Part2Export {
        val key = deviceKey()
        val drafts =
            dao.allDrafts().mapNotNull { row ->
                try {
                    Part2DraftBackup(row.id, Base64.encode(row.toDraft(key).toEntity(password).ciphertext))
                } catch (e: Exception) {
                    null
                }
            }
        val orders =
            dao.allOrders().mapNotNull { row ->
                try {
                    Part2OrderBackup(row.id, Base64.encode(row.toOrder(key).toEntity(password).ciphertext))
                } catch (e: Exception) {
                    null
                }
            }
        // One page at a time: only a page's row, its plaintext and its new ciphertext are ever held together.
        val pages =
            dao.allPageRefs().mapNotNull { ref ->
                val row = dao.page(ref.id) ?: return@mapNotNull null
                try {
                    val jpeg = row.openPage(key)
                    try {
                        Part2PageBackup(ref.id, ref.orderId, ref.position, Base64.encode(sealPage(ref.id, ref.orderId, ref.position, jpeg, password).ciphertext))
                    } finally {
                        jpeg.fill(0)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        return Part2Export(drafts, orders, pages)
    }

    suspend fun restore(
        backup: PortableBackup,
        password: String,
        mode: RestoreMode,
    ): Part2Restore {
        val key = deviceKey()
        val merge = mode == RestoreMode.MERGE
        val draftPass = pass(draftKind, backup.part2Drafts, if (merge) dao.allDrafts() else emptyList(), password, key, merge)
        val orderPass = pass(orderKind, backup.part2Orders, if (merge) dao.allOrders() else emptyList(), password, key, merge)
        val orderIds = orderPass.kept + orderPass.heldIds
        val pagePass = restorePages(backup.part2Pages, orderIds, password, key, merge)
        return Part2Restore(draftPass.rows, orderPass.rows, pagePass.first, draftPass.skipped + orderPass.skipped + pagePass.second)
    }

    private class Pass<E>(val rows: List<E>, val skipped: Int, val kept: Set<String>, val heldIds: Set<String>)

    /** What differs between drafts and orders: the caps, how a row is opened, and how an opened item is sealed again. */
    private class Kind<R, T, E>(
        val max: Int,
        val id: (R) -> String,
        val data: (R) -> String,
        val deviceId: (E) -> String,
        val fromBackup: (String, ByteArray, String) -> T,
        val fromDevice: (E, String) -> T,
        val itemId: (T) -> String,
        val updatedAt: (T) -> Long,
        val seal: (T, String) -> E,
    )

    private val textMax = base64Length(Part2Limits.PAYLOAD_MAX_BYTES + Part2Limits.SEAL_OVERHEAD_BYTES)

    private val draftKind =
        Kind<Part2DraftBackup, com.payslipmax.pdfparser.part2.domain.Part2Draft, Part2DraftEntity>(
            Part2Limits.DRAFTS_MAX, { it.id }, { it.data }, { it.id },
            { id, bytes, password -> Part2DraftEntity(id, bytes).toDraft(password) }, { row, key -> row.toDraft(key) },
            { it.id }, { it.updatedAt }, { item, key -> item.toEntity(key) },
        )

    private val orderKind =
        Kind<Part2OrderBackup, com.payslipmax.pdfparser.part2.domain.VaultOrder, Part2OrderEntity>(
            Part2Limits.ORDERS_MAX, { it.id }, { it.data }, { it.id },
            { id, bytes, password -> Part2OrderEntity(id, bytes).toOrder(password) }, { row, key -> row.toOrder(key) },
            { it.id }, { it.updatedAt }, { item, key -> item.toEntity(key) },
        )

    private fun <R, T, E> pass(
        kind: Kind<R, T, E>,
        rows: List<R>,
        device: List<E>,
        password: String,
        key: String,
        merge: Boolean,
    ): Pass<E> {
        val held = device.associateBy(kind.deviceId)
        var room = kind.max - device.size
        var skipped = (rows.size - kind.max).coerceAtLeast(0)
        val seen = mutableSetOf<String>()
        val kept = mutableListOf<E>()
        val keptIds = mutableSetOf<String>()
        for (row in rows.take(kind.max)) {
            val id = kind.id(row)
            val isNew = id !in held
            if (!Part2Limits.isPlainId(id) || kind.data(row).length > textMax || !seen.add(id) || (isNew && room <= 0)) {
                skipped++
                continue
            }
            val item =
                try {
                    kind.fromBackup(id, decodeText(kind.data(row)), password)
                } catch (e: Exception) {
                    null
                }
            if (item == null) {
                skipped++
                continue
            }
            val onDevice =
                held[id]?.let {
                    try {
                        kind.fromDevice(it, key)
                    } catch (e: Exception) {
                        null
                    }
                }
            if (merge && onDevice != null && kind.updatedAt(onDevice) > kind.updatedAt(item)) continue
            if (isNew) room--
            kept += kind.seal(item, key)
            keptIds += kind.itemId(item)
        }
        return Pass(kept, skipped, keptIds, held.keys)
    }

    private suspend fun restorePages(
        rows: List<Part2PageBackup>,
        orderIds: Set<String>,
        password: String,
        key: String,
        merge: Boolean,
    ): Pair<List<Part2PageEntity>, Int> {
        val max = Part2Limits.ORDERS_MAX * Part2Limits.PAGES_PER_ORDER_MAX
        val held = if (merge) dao.allPageRefs() else emptyList()
        val perOrder = held.groupingBy { it.orderId }.eachCount().toMutableMap()
        var vault = if (merge) dao.vaultBytes() else 0L
        var skipped = (rows.size - max).coerceAtLeast(0)
        val seen = held.map { it.id }.toMutableSet()
        val kept = mutableListOf<Part2PageEntity>()
        for (row in rows.take(max)) {
            val tooBig = row.data.length > base64Length(Part2Limits.PAGE_MAX_BYTES + 3 + row.id.length + row.orderId.length + Part2Limits.SEAL_OVERHEAD_BYTES)
            if (!Part2Limits.isPlainId(row.id) || !Part2Limits.isPlainId(row.orderId) || row.position < 0 || tooBig || row.orderId !in orderIds) {
                skipped++
                continue
            }
            if (row.id in seen) {
                // A page already on the device is the same immutable page, left alone; a repeat inside the file is a defect.
                if (row.id !in held.map { it.id }) skipped++
                continue
            }
            val estimate = row.data.length / 4 * 3
            if ((perOrder[row.orderId] ?: 0) >= Part2Limits.PAGES_PER_ORDER_MAX || vault + estimate > vaultBytesMax) {
                skipped++
                continue
            }
            val page = reseal(row, password, key)
            if (page == null) {
                skipped++
                continue
            }
            seen += row.id
            kept += page
            vault += page.ciphertext.size
            perOrder[row.orderId] = (perOrder[row.orderId] ?: 0) + 1
        }
        return kept to skipped
    }

    private fun reseal(
        row: Part2PageBackup,
        password: String,
        key: String,
    ): Part2PageEntity? =
        try {
            val jpeg = Part2PageEntity(row.id, row.orderId, row.position, decodeText(row.data)).openPage(password)
            try {
                sealPage(row.id, row.orderId, row.position, jpeg, key)
            } finally {
                jpeg.fill(0)
            }
        } catch (e: Exception) {
            null
        }

    private fun base64Length(bytes: Int) = (bytes + 2) / 3 * 4
}
