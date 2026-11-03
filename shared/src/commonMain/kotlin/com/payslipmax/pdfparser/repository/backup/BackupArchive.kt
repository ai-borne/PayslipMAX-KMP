package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.ArchiveKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.PasswordKey
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.part2.data.PageArchive
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.from

/**
 * A `.pcda` file opened with its password: the decrypted document, the key its rows and pages are sealed with, and [pages], the page
 * archive (a v6 file's body, or the whole older file). The caller closes it, which wipes a v6 key.
 */
internal class OpenedArchive(
    val document: ByteArray,
    val key: SealKey,
    val pages: BackupSource,
) : AutoCloseable {
    override fun close() = key.close()
}

/**
 * Opens [source], dispatching on its header (docs/Plan/14a P3). A v6 file's header is checked first, so a later app's file is refused with
 * `UnsupportedBackupVersionException` and a header out of bounds with `BackupDamagedException`, both before the KDF runs. A file without a
 * header is formats 1 to 5, read exactly as before: rows sealed per row with the password. A wrong password, a header changed after writing,
 * or a file that is not a backup fails with `DecryptionFailedException`.
 */
internal fun openArchive(
    source: BackupSource,
    password: String,
): OpenedArchive {
    val header = BackupEnvelope.read(source)
    if (header == null) {
        val meta = PageArchive.metaOf(source) ?: wholeBlob(source)
        return OpenedArchive(CryptoHelper.decrypt(meta, password).getOrThrow(), PasswordKey(password), source)
    }
    val body = source.from(BackupEnvelope.HEADER_BYTES.toLong())
    val meta = PageArchive.metaOf(body) ?: throw BackupDamagedException()
    val key = ArchiveKey.derive(password, header.salt, header.iterations, header.bytes)
    val document = key.open(meta).onFailure { key.close() }.getOrThrow()
    return OpenedArchive(document, key, body)
}

/** A new v6 archive: its header (fresh salt, [BackupEnvelope.ITERATIONS]) and the one key it derives, which the caller closes. */
internal class NewArchive(
    val header: ByteArray,
    val key: ArchiveKey,
) : AutoCloseable {
    override fun close() = key.close()
}

internal fun newArchive(password: String): NewArchive {
    val salt = CryptoHelper.randomBytes(BackupEnvelope.SALT_BYTES)
    val header = BackupEnvelope.header(PortableBackup.CURRENT_VERSION, BackupEnvelope.ITERATIONS, salt)
    return NewArchive(header, ArchiveKey.derive(password, salt, BackupEnvelope.ITERATIONS, header))
}

/** A file that is not a page archive is the one encrypted blob (payslips and their PDFs), which is read whole; no real one is this large. */
private fun wholeBlob(source: BackupSource): ByteArray {
    if (source.size > BLOB_MAX_BYTES) throw BackupDamagedException()
    return source.read(0, source.size.toInt())
}

/** The most a backup without scanned pages may be: payslips and their PDFs, a few megabytes in real use. */
private const val BLOB_MAX_BYTES = 128L * 1024 * 1024
