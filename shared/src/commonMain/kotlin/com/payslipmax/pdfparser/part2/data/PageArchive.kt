package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The shape of a `.pcda` archive that carries scanned pages (backup v5). A backup used to be one JSON string, so a vault of N bytes was
 * copied through Base64, a JSON builder, UTF-8 and the encryptor: about 13 N of memory (measured). A page archive is instead
 *
 * ```
 * "PCDB" | u32 length | metadata document (the encrypted PortableBackup, as before)
 * { u8 idLength | id | u8 orderIdLength | orderId | u32 position | u32 length | sealed page } ...
 * encrypted trailer {"pages": n} | u32 trailer length
 * ```
 *
 * with every number big-endian. A sealed page is the same ciphertext the database holds, made for the backup password, so the archive is
 * the vault's size plus a few hundred bytes, is written into one buffer of the final size, and is read one record at a time. The page
 * count lives in the authenticated trailer, so a record cut out or added is caught. A vault with no pages still writes the plain
 * encrypted document (nothing here is involved), and the magic cannot be the start of one of those: they begin with a random salt, and a
 * file whose first four bytes happen to match still has to pass every length check below.
 */
object PageArchive {
    private val MAGIC = "PCDB".encodeToByteArray()
    private const val HEAD = 8
    private const val LENGTH_BYTES = 4
    private const val RECORD_FIXED = 1 + 1 + 4 + 4

    /** The records run from [recordsStart] to [trailerStart], where the encrypted trailer begins. */
    internal class Layout(
        val metaLength: Int,
        val recordsStart: Int,
        val trailerStart: Int,
        val trailerLength: Int,
    )

    /** The metadata document of a page archive (the encrypted `PortableBackup`), or null when [bytes] is not one. */
    fun metaOf(bytes: ByteArray): ByteArray? = layout(bytes)?.let { bytes.copyOfRange(HEAD, HEAD + it.metaLength) }

    /** The regions of a page archive, judged from its own lengths only (nothing is decrypted); null when anything points outside the file. */
    internal fun layout(bytes: ByteArray): Layout? {
        if (bytes.size < HEAD + LENGTH_BYTES || !MAGIC.indices.all { bytes[it] == MAGIC[it] }) return null
        val metaLength = u32(bytes, MAGIC.size) ?: return null
        val trailerLength = u32(bytes, bytes.size - LENGTH_BYTES) ?: return null
        // Long arithmetic: both lengths come from the file, and 8 + Int.MAX_VALUE would wrap around to a negative offset.
        val recordsStart = HEAD.toLong() + metaLength
        val trailerStart = bytes.size.toLong() - LENGTH_BYTES - trailerLength
        if (recordsStart > trailerStart) return null
        return Layout(metaLength, recordsStart.toInt(), trailerStart.toInt(), trailerLength)
    }

    /** A big-endian length at [at], or null when it does not fit in an Int (a file that lies). */
    internal fun u32(
        bytes: ByteArray,
        at: Int,
    ): Int? {
        if (at < 0 || at + LENGTH_BYTES > bytes.size) return null
        val value = (0 until LENGTH_BYTES).fold(0L) { n, i -> (n shl 8) or (bytes[at + i].toLong() and 0xFF) }
        return if (value > Int.MAX_VALUE) null else value.toInt()
    }

    internal fun trailer(
        count: Int,
        password: String,
    ): ByteArray =
        CryptoHelper.encrypt(Json.encodeToString(PageTrailer(count)).encodeToByteArray(), password).getOrThrow()

    internal fun recordSize(
        ref: com.payslipmax.pdfparser.part2.domain.VaultPageRef,
        sealedBytes: Int,
    ): Int = RECORD_FIXED + ref.id.length + ref.orderId.length + sealedBytes

    /** The exact size of an archive with these pages, so the writer's one buffer is the final array. */
    internal fun capacity(
        metaLength: Int,
        refs: List<com.payslipmax.pdfparser.part2.domain.VaultPageRef>,
        sealedTotal: Long,
        count: Int,
    ): Int {
        val records = refs.sumOf { RECORD_FIXED + it.id.length + it.orderId.length }.toLong() + sealedTotal
        return (HEAD + metaLength + records + trailer(count, "size-probe").size + LENGTH_BYTES).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /** The largest sealed page a record with these ids may carry; a record claiming more is refused before it is read. */
    internal fun sealedMax(
        id: String,
        orderId: String,
    ): Int = Part2Limits.PAGE_MAX_BYTES + 3 + id.length + orderId.length + Part2Limits.SEAL_OVERHEAD_BYTES
}

/** The authenticated page count at the end of a page archive. */
@Serializable
internal class PageTrailer(
    val pages: Int,
)

/** Builds a page archive into one buffer of [capacity] bytes (see [PageArchive.capacity]); it never grows, so there is no second copy. */
class PageArchiveWriter(
    capacity: Int,
) {
    private val buffer = ByteArray(capacity)
    private var at = 0
    private var begun = false

    fun begin(meta: ByteArray) {
        check(!begun) { "The archive document is already written." }
        put("PCDB".encodeToByteArray())
        putU32(meta.size)
        put(meta)
        begun = true
    }

    fun add(
        id: String,
        orderId: String,
        position: Int,
        sealed: ByteArray,
    ) {
        check(begun) { "The archive document must be written first." }
        val idBytes = id.encodeToByteArray()
        val orderBytes = orderId.encodeToByteArray()
        require(idBytes.size in 1..255 && orderBytes.size in 1..255) { "A page id is a plain token of at most 255 bytes." }
        buffer[room(1)] = idBytes.size.toByte()
        put(idBytes)
        buffer[room(1)] = orderBytes.size.toByte()
        put(orderBytes)
        putU32(position)
        putU32(sealed.size)
        put(sealed)
    }

    /** Writes the trailer, which says there are [count] pages, and returns the archive (the buffer itself when it was sized exactly). */
    fun finish(
        count: Int,
        password: String,
    ): ByteArray {
        check(begun) { "The archive document must be written first." }
        val trailer = PageArchive.trailer(count, password)
        put(trailer)
        putU32(trailer.size)
        return if (at == buffer.size) buffer else buffer.copyOf(at)
    }

    private fun room(n: Int): Int {
        check(at + n <= buffer.size) { "The archive buffer is full." }
        return at.also { at += n }
    }

    private fun put(bytes: ByteArray) {
        bytes.copyInto(buffer, room(bytes.size))
    }

    private fun putU32(value: Int) {
        val start = room(4)
        for (i in 0 until 4) buffer[start + i] = (value ushr (24 - 8 * i)).toByte()
    }
}
