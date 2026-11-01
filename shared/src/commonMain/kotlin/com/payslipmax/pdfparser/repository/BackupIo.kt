package com.payslipmax.pdfparser.repository

/**
 * Where a backup is written: bytes are appended in order and never read back. A platform's sink is a file, so a backup of any size passes
 * through a few hundred kilobytes of memory; [MemoryBackupSink] keeps the bytes for small files and tests.
 */
fun interface BackupSink {
    fun write(bytes: ByteArray)
}

/**
 * Where a backup is read from: any range, by offset, so a restore never needs the whole file in memory. A range outside the file is a
 * bug in the caller (the archive's own lengths are checked first) and throws [IllegalArgumentException].
 */
interface BackupSource {
    val size: Long

    fun read(
        offset: Long,
        length: Int,
    ): ByteArray
}

/** A sink that holds a file open: close it to flush and release the file before the file is shared or deleted. */
interface ClosableBackupSink : BackupSink, AutoCloseable

/** A source that holds a file open: close it to release the file before the file is deleted. */
interface ClosableBackupSource : BackupSource, AutoCloseable

/** Collects what is written, in chunks, and joins them once in [toByteArray]. */
class MemoryBackupSink : BackupSink {
    private val chunks = mutableListOf<ByteArray>()

    override fun write(bytes: ByteArray) {
        chunks += bytes.copyOf()
    }

    fun toByteArray(): ByteArray {
        val out = ByteArray(chunks.sumOf { it.size })
        var at = 0
        for (chunk in chunks) {
            chunk.copyInto(out, at)
            at += chunk.size
        }
        return out
    }
}

/** A backup already in memory. */
class MemoryBackupSource(private val bytes: ByteArray) : BackupSource {
    override val size: Long get() = bytes.size.toLong()

    override fun read(
        offset: Long,
        length: Int,
    ): ByteArray {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) { "A range outside the backup." }
        return bytes.copyOfRange(offset.toInt(), offset.toInt() + length)
    }
}
