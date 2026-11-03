package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.repository.BackupFileException
import com.payslipmax.pdfparser.repository.BackupFiles
import com.payslipmax.pdfparser.repository.BackupSink
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.MemoryBackupSink
import com.payslipmax.pdfparser.repository.MemoryBackupSource

/**
 * A backup file kept in memory, with a count of everything the real files do (written, shared, deleted), so a test can say
 * "the file is gone after a failure" without a disk. [stage] stands in for the picker: it returns the key of a picked file.
 */
class FakeBackupFiles : BackupFiles {
    private var exported: ByteArray? = null
    private val picked = mutableMapOf<String, ByteArray>()
    private var next = 0

    var shareCount = 0
        private set
    var shareFails = false
    var sweepCount = 0
        private set

    /** The export file as written and not yet deleted; null once it was deleted or never written. */
    val exportedBytes: ByteArray? get() = exported

    /** Keys of picked files still on "disk". */
    val pickedKeys: Set<String> get() = picked.keys

    fun stage(bytes: ByteArray): String = "picked-${next++}".also { picked[it] = bytes }

    /** When set, this byte of the written file is flipped before it is read back: a disk that did not keep what was written. */
    var corruptWrittenByteAt: Int? = null

    /** How many times a written file was read back. */
    var verifyCount = 0
        private set

    override suspend fun write(
        verify: suspend (BackupSource) -> Result<Unit>,
        block: suspend (BackupSink) -> Result<Unit>,
    ): Result<Unit> {
        exported = null
        val sink = MemoryBackupSink()
        val written = runCatching { block(sink) }.getOrElse { Result.failure(BackupFileException()) }
        if (written.isFailure) return written
        val bytes = sink.toByteArray()
        corruptWrittenByteAt?.let { bytes[it] = (bytes[it].toInt() xor 0x01).toByte() }
        verifyCount++
        val checked = runCatching { verify(MemoryBackupSource(bytes)) }.getOrElse { Result.failure(BackupFileException()) }
        if (checked.isSuccess) exported = bytes
        return checked
    }

    override fun share(): Result<Unit> {
        if (shareFails) {
            exported = null
            return Result.failure(BackupFileException())
        }
        shareCount++
        return Result.success(Unit)
    }

    override suspend fun <R> read(
        picked: String,
        block: suspend (BackupSource) -> Result<R>,
    ): Result<R> {
        val bytes = this.picked.remove(picked) ?: return Result.failure(BackupFileException())
        return runCatching { block(MemoryBackupSource(bytes)) }.getOrElse { Result.failure(BackupFileException()) }
    }

    override fun discard(picked: String) {
        this.picked.remove(picked)
    }

    override fun sweep() {
        sweepCount++
        exported = null
        picked.clear()
    }
}
