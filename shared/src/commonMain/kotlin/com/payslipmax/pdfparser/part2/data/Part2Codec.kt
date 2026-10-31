package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Turns entries, orders and pages into encrypted rows and back, the way `GuideNoteEntity` does for notes: one AES-GCM ciphertext per
 * row, the id repeated inside it, and every failure becoming the same fixed exception without a cause (serialization errors quote the
 * input, and the input is private). Nothing here logs.
 */
private val payloadJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/** Encrypts [json] under [key], or [Part2TooLargeException] when it passes `Part2Limits.PAYLOAD_MAX_BYTES`. The plaintext bytes are wiped. */
internal fun sealPayload(
    json: String,
    key: String,
): ByteArray {
    val plain = json.encodeToByteArray()
    try {
        if (plain.size > Part2Limits.PAYLOAD_MAX_BYTES) throw Part2TooLargeException()
        return CryptoHelper.encrypt(plain, key).getOrThrow()
    } finally {
        plain.fill(0)
    }
}

private fun <T> unreadable(read: () -> T?): T =
    (
        try {
            read()
        } catch (e: Exception) {
            null
        }
    ) ?: throw Part2UnreadableException()

private inline fun <reified P> decodePayload(
    sealed: ByteArray,
    key: String,
): P {
    val plain = CryptoHelper.decrypt(sealed, key).getOrThrow()
    try {
        return payloadJson.decodeFromString<P>(plain.decodeToString())
    } finally {
        plain.fill(0)
    }
}

internal fun BankEntry.toEntity(key: String) = Part2EntryEntity(id, sealPayload(payloadJson.encodeToString(toPayload()), key))

/** The entry this row holds, or [Part2UnreadableException]. The bounds apply to stored data too, so a restored file cannot exceed them. */
internal fun Part2EntryEntity.toEntry(key: String): BankEntry =
    unreadable {
        val payload = decodePayload<EntryPayload>(ciphertext, key)
        if (payload.v != PAYLOAD_VERSION || payload.id != id) null else payload.toEntry().takeIf { Part2Bounds.check(it) == null }
    }

internal fun VaultOrder.toEntity(key: String) = Part2OrderEntity(id, sealPayload(payloadJson.encodeToString(toPayload()), key))

internal fun Part2OrderEntity.toOrder(key: String): VaultOrder =
    unreadable {
        val payload = decodePayload<OrderPayload>(ciphertext, key)
        if (payload.v != PAYLOAD_VERSION || payload.id != id) null else payload.toOrder().takeIf { Part2Bounds.check(it) == null }
    }

// A page is binary, so it is not wrapped in JSON (Base64 would add a third): version byte, id length, id, order id length, order id, JPEG.
// Both ids are inside the ciphertext for the same reason as above. Ids are ASCII tokens, so characters and bytes are the same count.
private const val PAGE_FRAME_VERSION: Byte = 1

internal fun sealPage(
    id: String,
    orderId: String,
    position: Int,
    jpeg: ByteArray,
    key: String,
): Part2PageEntity {
    require(Part2Limits.isPlainId(id) && Part2Limits.isPlainId(orderId) && position >= 0) { "A Part II page needs plain ids and a position." }
    if (jpeg.isEmpty() || jpeg.size > Part2Limits.PAGE_MAX_BYTES) throw Part2TooLargeException()
    val a = id.encodeToByteArray()
    val b = orderId.encodeToByteArray()
    val frame = ByteArray(3 + a.size + b.size + jpeg.size)
    frame[0] = PAGE_FRAME_VERSION
    frame[1] = a.size.toByte()
    a.copyInto(frame, 2)
    frame[2 + a.size] = b.size.toByte()
    b.copyInto(frame, 3 + a.size)
    jpeg.copyInto(frame, 3 + a.size + b.size)
    try {
        return Part2PageEntity(id, orderId, position, CryptoHelper.encrypt(frame, key).getOrThrow())
    } finally {
        frame.fill(0)
    }
}

/** The page's JPEG, which the caller must wipe when done, or [Part2UnreadableException]. */
internal fun Part2PageEntity.openPage(key: String): ByteArray {
    val frame =
        try {
            CryptoHelper.decrypt(ciphertext, key).getOrThrow()
        } catch (e: Exception) {
            throw Part2UnreadableException()
        }
    try {
        return unreadable { jpegOf(frame) }
    } finally {
        frame.fill(0)
    }
}

private fun Part2PageEntity.jpegOf(frame: ByteArray): ByteArray? {
    if (frame.size < 3 || frame[0] != PAGE_FRAME_VERSION) return null
    val idLength = frame[1].toInt() and 0xFF
    val orderAt = 2 + idLength
    if (idLength !in 1..Part2Limits.ID_MAX || orderAt >= frame.size) return null
    val orderLength = frame[orderAt].toInt() and 0xFF
    val jpegAt = orderAt + 1 + orderLength
    if (orderLength !in 1..Part2Limits.ID_MAX || jpegAt >= frame.size) return null
    val sameIds = frame.decodeToString(2, orderAt) == id && frame.decodeToString(orderAt + 1, jpegAt) == orderId
    val size = frame.size - jpegAt
    return if (sameIds && size <= Part2Limits.PAGE_MAX_BYTES) frame.copyOfRange(jpegAt, frame.size) else null
}
