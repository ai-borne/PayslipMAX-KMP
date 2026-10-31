package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder

/** What a restore keeps of one kind of row, already re-encrypted for this device; [kept] and [heldIds] say which ids exist afterwards. */
internal class RowPass<E>(
    val rows: List<E>,
    val skipped: Int,
    val kept: Set<String>,
    val heldIds: Set<String>,
)

/** What differs between entries and orders: the caps, how a row is opened, and how an opened item is sealed again. */
internal class RowKind<R, T, E>(
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

internal fun base64Length(bytes: Int) = (bytes + 2) / 3 * 4

private val TEXT_MAX = base64Length(Part2Limits.PAYLOAD_MAX_BYTES + Part2Limits.SEAL_OVERHEAD_BYTES)

internal val entryKind =
    RowKind<Part2RowBackup, BankEntry, Part2EntryEntity>(
        Part2Limits.ENTRIES_MAX,
        { it.id },
        { it.data },
        { it.id },
        { id, bytes, password -> Part2EntryEntity(id, bytes).toEntry(password) },
        { row, key -> row.toEntry(key) },
        { it.id },
        { it.updatedAt },
        { item, key -> item.toEntity(key) },
    )

internal val orderKind =
    RowKind<Part2RowBackup, VaultOrder, Part2OrderEntity>(
        Part2Limits.ORDERS_MAX,
        { it.id },
        { it.data },
        { it.id },
        { id, bytes, password -> Part2OrderEntity(id, bytes).toOrder(password) },
        { row, key -> row.toOrder(key) },
        { it.id },
        { it.updatedAt },
        { item, key -> item.toEntity(key) },
    )

/**
 * Restores the entry or order rows of an archive. Each row's id shape, text length and count are checked before it is decoded (a row
 * past [RowKind.max] is not even looked at); on MERGE the newer `updatedAt` wins and a tie goes to the backup; a new row fits only in the
 * room the device has left. Anything that fails is skipped and counted.
 */
internal fun <R, T, E> restoreRows(
    kind: RowKind<R, T, E>,
    rows: List<R>,
    device: List<E>,
    password: String,
    key: String,
    merge: Boolean,
    decodeText: (String) -> ByteArray,
): RowPass<E> {
    val held = device.associateBy(kind.deviceId)
    var room = kind.max - device.size
    var skipped = (rows.size - kind.max).coerceAtLeast(0)
    val seen = mutableSetOf<String>()
    val kept = mutableListOf<E>()
    val keptIds = mutableSetOf<String>()
    for (row in rows.take(kind.max)) {
        val id = kind.id(row)
        val isNew = id !in held
        if (!Part2Limits.isPlainId(id) || kind.data(row).length > TEXT_MAX || !seen.add(id) || (isNew && room <= 0)) {
            skipped++
            continue
        }
        val item = runCatching { kind.fromBackup(id, decodeText(kind.data(row)), password) }.getOrNull()
        if (item == null) {
            skipped++
            continue
        }
        val onDevice = held[id]?.let { runCatching { kind.fromDevice(it, key) }.getOrNull() }
        if (merge && onDevice != null && kind.updatedAt(onDevice) > kind.updatedAt(item)) continue
        if (isNew) room--
        kept += kind.seal(item, key)
        keptIds += kind.itemId(item)
    }
    return RowPass(kept, skipped, keptIds, held.keys)
}
