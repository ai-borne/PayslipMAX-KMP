package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException

/** A v6 header as read: what derives the archive key, and its own bytes, which every seal in the file authenticates. */
internal class EnvelopeHeader(
    val iterations: Int,
    val formatVersion: Int,
    val salt: ByteArray,
    val bytes: ByteArray,
)

/**
 * The plaintext header that starts a format 6 `.pcda` file (F9, F10; docs/Plan/14a P3), all numbers big-endian:
 *
 * ```
 * "PCDX" | u8 envelope version | u8 KDF id | u32 iterations | u16 format version | u8 salt length | salt
 * ```
 *
 * followed by a `PageArchive` sealed with the one key the header derives. The header is read BEFORE anything is decrypted, so a file from a
 * later app answers "update the app" ([UnsupportedBackupVersionException]) and never a decrypt error, and its numbers are bounded before
 * the KDF runs, so a hostile file cannot stall it. A file without the magic is an older format and takes the legacy path.
 *
 * The one version check (SSOT): [read] for the header, [requireKnownFormat] for the document inside any file. P6 maps the exception.
 */
internal object BackupEnvelope {
    private val MAGIC = "PCDX".encodeToByteArray()
    const val VERSION = 1
    const val KDF_PBKDF2_SHA256 = 1
    const val ITERATIONS = 600_000
    const val ITERATIONS_MIN = 10_000
    const val ITERATIONS_MAX = 5_000_000
    const val SALT_BYTES = 16
    const val HEADER_BYTES = 4 + 1 + 1 + 4 + 2 + 1 + SALT_BYTES

    /** A header for a new archive of [formatVersion] with [salt]. */
    fun header(
        formatVersion: Int,
        iterations: Int,
        salt: ByteArray,
    ): ByteArray {
        require(salt.size == SALT_BYTES && iterations in ITERATIONS_MIN..ITERATIONS_MAX && formatVersion in 1..0xFFFF)
        return MAGIC + byteArrayOf(VERSION.toByte(), KDF_PBKDF2_SHA256.toByte()) + be(iterations.toLong(), 4) +
            be(formatVersion.toLong(), 2) + byteArrayOf(SALT_BYTES.toByte()) + salt
    }

    /**
     * The header of [source], or null when it has none (formats 1 to 5). Throws [UnsupportedBackupVersionException] for an envelope, KDF
     * or format this app does not know, and [BackupDamagedException] for a header cut short or with numbers outside its bounds.
     */
    fun read(source: BackupSource): EnvelopeHeader? {
        if (source.size < MAGIC.size) return null
        val head = source.read(0, minOf(source.size, HEADER_BYTES.toLong()).toInt())
        if (!MAGIC.indices.all { head[it] == MAGIC[it] }) return null
        if (head.size <= 4) throw BackupDamagedException()
        val envelope = head[4].toInt() and 0xFF
        if (envelope != VERSION) throw UnsupportedBackupVersionException(envelope)
        if (head.size < HEADER_BYTES) throw BackupDamagedException()
        val format = number(head, 10, 2).toInt()
        requireKnownFormat(format)
        if ((head[5].toInt() and 0xFF) != KDF_PBKDF2_SHA256) throw UnsupportedBackupVersionException(format)
        val iterations = number(head, 6, 4)
        if (iterations !in ITERATIONS_MIN..ITERATIONS_MAX || (head[12].toInt() and 0xFF) != SALT_BYTES) throw BackupDamagedException()
        return EnvelopeHeader(iterations.toInt(), format, head.copyOfRange(13, HEADER_BYTES), head)
    }

    /** A document or header of a format later than this app's is refused with "update the app", before anything in it is used. */
    fun requireKnownFormat(version: Int) {
        if (version > PortableBackup.CURRENT_VERSION) throw UnsupportedBackupVersionException(version)
    }

    private fun be(
        value: Long,
        width: Int,
    ): ByteArray = ByteArray(width) { (value ushr (8 * (width - 1 - it))).toByte() }

    private fun number(
        bytes: ByteArray,
        at: Int,
        width: Int,
    ): Long = (0 until width).fold(0L) { n, i -> (n shl 8) or (bytes[at + i].toLong() and 0xFF) }
}
