package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BackupSink
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.MemoryBackupSource
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
 * the vault's size plus a few hundred bytes, is written to a [BackupSink] one record at a time, and is read from a [BackupSource] by
 * range, so an archive of any size passes through a few hundred kilobytes of memory. The page count lives in the authenticated trailer, so a record cut out or added is caught. A vault with no pages still writes the plain
 * encrypted document (nothing here is involved), and the magic cannot be the start of one of those: they begin with a random salt, and a
 * file whose first four bytes happen to match still has to pass every length check below.
 */
object PageArchive {
    private val MAGIC = "PCDB".encodeToByteArray()
    private const val HEAD = 8
    private const val LENGTH_BYTES = 4

    /** The records run from [recordsStart] to [trailerStart], where the encrypted trailer begins. Offsets are Long: a file is not an array. */
    internal class Layout(
        val metaLength: Int,
        val recordsStart: Long,
        val trailerStart: Long,
        val trailerLength: Int,
    )

    /** The metadata document of a page archive (the encrypted `PortableBackup`), or null when [source] is not one. */
    fun metaOf(source: BackupSource): ByteArray? = layout(source)?.let { source.read(HEAD.toLong(), it.metaLength) }

    fun metaOf(bytes: ByteArray): ByteArray? = metaOf(MemoryBackupSource(bytes))

    /** The regions of a page archive, judged from its own lengths only (the first 8 and last 4 bytes are read; nothing is decrypted); null when anything points outside the file. */
    internal fun layout(source: BackupSource): Layout? {
        val size = source.size
        if (size < HEAD + LENGTH_BYTES) return null
        val head = source.read(0, HEAD)
        if (!MAGIC.indices.all { head[it] == MAGIC[it] }) return null
        val metaLength = u32(head, MAGIC.size) ?: return null
        val trailerLength = u32(source.read(size - LENGTH_BYTES, LENGTH_BYTES), 0) ?: return null
        // Long arithmetic: both lengths come from the file, and 8 + Int.MAX_VALUE would wrap around to a negative offset.
        val recordsStart = HEAD.toLong() + metaLength
        val trailerStart = size - LENGTH_BYTES - trailerLength
        if (recordsStart > trailerStart) return null
        return Layout(metaLength, recordsStart, trailerStart, trailerLength)
    }

    internal fun layout(bytes: ByteArray): Layout? = layout(MemoryBackupSource(bytes))

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

/**
 * Writes a page archive to a [sink], one record at a time: the document first, then each page, then the trailer that counts them. Nothing
 * larger than one sealed page is ever written in one piece, and nothing is kept.
 */
class PageArchiveWriter(
    private val sink: BackupSink,
) {
    private var begun = false

    fun begin(meta: ByteArray) {
        check(!begun) { "The archive document is already written." }
        sink.write("PCDB".encodeToByteArray() + u32(meta.size))
        sink.write(meta)
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
        sink.write(byteArrayOf(idBytes.size.toByte()) + idBytes + byteArrayOf(orderBytes.size.toByte()) + orderBytes + u32(position) + u32(sealed.size))
        sink.write(sealed)
    }

    /** Writes the trailer, which says there are [count] pages. */
    fun finish(
        count: Int,
        password: String,
    ) {
        check(begun) { "The archive document must be written first." }
        val trailer = PageArchive.trailer(count, password)
        sink.write(trailer)
        sink.write(u32(trailer.size))
    }

    private fun u32(value: Int): ByteArray = ByteArray(4) { (value ushr (24 - 8 * it)).toByte() }
}
