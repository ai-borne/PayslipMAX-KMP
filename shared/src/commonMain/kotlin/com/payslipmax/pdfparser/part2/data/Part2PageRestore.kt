package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.backup.BackupJson

/** The pages a restore will write ([pages] is lazy) and how many records it left out. */
internal class PageRestore(
    val pages: Sequence<Part2PageEntity>,
    val skipped: Int,
)

private class Accepted(
    val id: String,
    val orderId: String,
    val position: Int,
    val offset: Long,
    val length: Int,
)

/** Everything a record's header says, read without touching the page. */
private class Header(
    val id: String,
    val orderId: String,
    val position: Int?,
    val length: Int,
    val dataAt: Long,
)

/**
 * Restores the pages of a page archive from a [BackupSource] by range, without ever holding the vault (or the file) in memory. The archive's own lengths and its authenticated page count
 * are checked first (any lie fails the whole restore with [BackupDamagedException] before anything is written); then each record's
 * header is judged against `Part2Limits` BEFORE its page is read: ids, size, its order, the ten-per-order count and the vault
 * ceiling. A record that passes is verified (decrypted and its ids compared, one page at a time, then wiped); the survivors are
 * re-encrypted for this device lazily, as the transaction inserts them. A record that fails is skipped and counted.
 */
internal suspend fun restorePages(
    dao: Part2Dao,
    source: BackupSource,
    orderIds: Set<String>,
    password: String,
    key: String,
    merge: Boolean,
    vaultBytesMax: Long,
    openPage: (Part2PageEntity, String) -> ByteArray,
): PageRestore {
    val layout = PageArchive.layout(source) ?: return PageRestore(emptySequence(), 0)
    val declared = declaredCount(source, layout, password)
    val examined = minOf(declared, Part2Limits.ORDERS_MAX * Part2Limits.PAGES_PER_ORDER_MAX)
    val held = if (merge) dao.allPageRefs() else emptyList()
    val heldIds = held.map { it.id }.toSet()
    val perOrder = held.groupingBy { it.orderId }.eachCount().toMutableMap()
    var vault = if (merge) dao.vaultBytes() else 0L
    var skipped = declared - examined
    val seen = mutableSetOf<String>()
    val accepted = mutableListOf<Accepted>()
    var at = layout.recordsStart
    repeat(examined) {
        val header = readHeader(source, at, layout.trailerStart) ?: throw BackupDamagedException()
        at = header.dataAt + header.length
        val limit = PageArchive.sealedMax(header.id, header.orderId)
        val position = header.position
        val fits = Part2Limits.isPlainId(header.id) && Part2Limits.isPlainId(header.orderId) && position != null && header.length in 1..limit
        when {
            header.id in heldIds -> Unit // an immutable page the device already holds: left alone, not an error
            !fits || header.orderId !in orderIds || !seen.add(header.id) -> skipped++
            (perOrder[header.orderId] ?: 0) >= Part2Limits.PAGES_PER_ORDER_MAX || vault + header.length > vaultBytesMax -> skipped++
            !verified(source, header, position, password, openPage) -> skipped++
            else -> {
                accepted += Accepted(header.id, header.orderId, position, header.dataAt, header.length)
                vault += header.length
                perOrder[header.orderId] = (perOrder[header.orderId] ?: 0) + 1
            }
        }
    }
    // Every record the trailer counts must be there and nothing may follow them: a count that does not match is a damaged file.
    if (declared <= Part2Limits.ORDERS_MAX * Part2Limits.PAGES_PER_ORDER_MAX && at != layout.trailerStart) throw BackupDamagedException()
    val pages =
        accepted.asSequence().map { page ->
            val sealed = Part2PageEntity(page.id, page.orderId, page.position, source.read(page.offset, page.length))
            val jpeg = openPage(sealed, password)
            try {
                sealPage(page.id, page.orderId, page.position, jpeg, key)
            } finally {
                jpeg.fill(0)
            }
        }
    return PageRestore(pages, skipped)
}

/** An encrypted `{"pages": n}` is about a hundred bytes; a trailer claiming more is a lie and is refused before it is read. */
private const val TRAILER_MAX_BYTES = 4096

private fun declaredCount(
    source: BackupSource,
    layout: PageArchive.Layout,
    password: String,
): Int =
    try {
        if (layout.trailerLength > TRAILER_MAX_BYTES) throw BackupDamagedException()
        val plain = com.payslipmax.pdfparser.crypto.CryptoHelper.decrypt(source.read(layout.trailerStart, layout.trailerLength), password).getOrThrow()
        BackupJson.decode<PageTrailer>(plain.decodeToString()).pages
    } catch (e: Exception) {
        throw BackupDamagedException()
    }

/** A record's header is at most 1 + 255 + 1 + 255 + 8 bytes; it is read as one small window and parsed from there. */
private const val HEADER_MAX_BYTES = 1 + 255 + 1 + 255 + 8

private fun readHeader(
    source: BackupSource,
    at: Long,
    end: Long,
): Header? {
    if (at >= end) return null
    val window = source.read(at, minOf(end - at, HEADER_MAX_BYTES.toLong()).toInt())
    val idLength = window[0].toInt() and 0xFF
    val orderAt = 1 + idLength
    if (idLength == 0 || orderAt >= window.size) return null
    val orderLength = window[orderAt].toInt() and 0xFF
    val positionAt = orderAt + 1 + orderLength
    if (orderLength == 0 || positionAt + 8 > window.size) return null
    val position = PageArchive.u32(window, positionAt)
    val length = PageArchive.u32(window, positionAt + 4) ?: return null
    val dataAt = at + positionAt + 8
    if (length.toLong() > end - dataAt) return null
    return Header(window.decodeToString(1, orderAt), window.decodeToString(orderAt + 1, positionAt), position, length, dataAt)
}

private fun verified(
    source: BackupSource,
    header: Header,
    position: Int,
    password: String,
    openPage: (Part2PageEntity, String) -> ByteArray,
): Boolean =
    try {
        val sealed = Part2PageEntity(header.id, header.orderId, position, source.read(header.dataAt, header.length))
        openPage(sealed, password).fill(0)
        true
    } catch (e: Part2UnreadableException) {
        false
    }
