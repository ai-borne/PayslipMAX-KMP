package com.payslipmax.pdfparser.repository

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/** Writes a backup to [file], starting it afresh. Close it to flush and release the file. */
class FileBackupSink(file: File) : ClosableBackupSink {
    private val out = BufferedOutputStream(FileOutputStream(file), BUFFER_BYTES)

    override fun write(bytes: ByteArray) = out.write(bytes)

    override fun close() = out.close()

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}

/** Reads a backup from [file] by range. Close it to release the file. */
class FileBackupSource(file: File) : ClosableBackupSource {
    private val input = RandomAccessFile(file, "r")

    override val size: Long = input.length()

    override fun read(
        offset: Long,
        length: Int,
    ): ByteArray {
        require(offset >= 0 && length >= 0 && offset + length <= size) { "A range outside the backup." }
        val out = ByteArray(length)
        input.seek(offset)
        input.readFully(out)
        return out
    }

    override fun close() = input.close()
}
