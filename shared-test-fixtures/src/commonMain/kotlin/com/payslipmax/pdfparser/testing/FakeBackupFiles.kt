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

    override suspend fun write(block: suspend (BackupSink) -> Result<Unit>): Result<Unit> {
        exported = null
        val sink = MemoryBackupSink()
        val result = runCatching { block(sink) }.getOrElse { Result.failure(BackupFileException()) }
        if (result.isSuccess) exported = sink.toByteArray()
        return result
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
