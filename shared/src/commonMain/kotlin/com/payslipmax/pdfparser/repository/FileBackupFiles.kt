package com.payslipmax.pdfparser.repository

import kotlin.coroutines.cancellation.CancellationException

/** The rules every platform shares; a platform supplies only where the files are and how to open, share and delete one. */
abstract class FileBackupFiles : BackupFiles {
    protected abstract val exportPath: String

    protected abstract fun leftovers(): List<String>

    protected abstract fun openSink(path: String): ClosableBackupSink

    protected abstract fun openSource(path: String): ClosableBackupSource

    protected abstract fun delete(path: String)

    protected abstract fun present(path: String)

    override suspend fun write(block: suspend (BackupSink) -> Result<Unit>): Result<Unit> {
        sweep()
        return guarded(onFailure = { delete(exportPath) }) { openSink(exportPath).use { block(it) } }
    }

    override fun share(): Result<Unit> = guarded(onFailure = { delete(exportPath) }) { Result.success(present(exportPath)) }

    override suspend fun <R> read(
        picked: String,
        block: suspend (BackupSource) -> Result<R>,
    ): Result<R> =
        try {
            guarded { openSource(picked).use { block(it) } }
        } finally {
            discard(picked)
        }

    override fun discard(picked: String) {
        runCatching { delete(picked) }
    }

    override fun sweep() {
        leftovers().forEach(::discard)
    }

    /**
     * Runs [body]; a result that failed, or an exception that escaped it, runs [onFailure] (the half-made file is removed). An exception
     * becomes a [BackupFileException] with a fixed message, because an I/O message can carry a path. Cancellation is passed on.
     */
    private inline fun <R> guarded(
        onFailure: () -> Unit = {},
        body: () -> Result<R>,
    ): Result<R> {
        var result: Result<R>? = null
        try {
            result = body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            result = Result.failure(BackupFileException())
        } finally {
            if (result?.isSuccess != true) runCatching { onFailure() }
        }
        return result
    }
}
