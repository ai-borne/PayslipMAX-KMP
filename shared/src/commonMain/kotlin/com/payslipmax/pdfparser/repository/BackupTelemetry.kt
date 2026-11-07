package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.telemetry.CrashReporter
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sends one non-fatal per failed backup or restore to Crashlytics: a fixed message and three keys holding fixed codes only
 * ([KEY_FAILURE], [KEY_FORMAT], [KEY_OP]); never the exception, its text, a path, a month or a row. Nothing is sent when the officer has
 * turned telemetry off in [settings] (no settings row yet is the app's default, on), or when the choice cannot be read.
 */
class BackupTelemetry(
    private val reporter: CrashReporter,
    private val settings: PayslipDao,
) {
    /**
     * Records [error]'s failure for [op]. [formatVersion] is the file's format when it is known (a backup writes the current one);
     * a later version, or a refusal for one, is "newer", and anything else "unknown", so the value is always one of a fixed set.
     */
    suspend fun report(
        op: BackupOp,
        error: Throwable?,
        formatVersion: Int? = null,
    ) {
        if (!consented()) return
        val failure = BackupFailure.of(error)
        val format =
            when {
                failure == BackupFailure.NEWER_VERSION -> NEWER
                formatVersion == null || formatVersion < 1 -> UNKNOWN
                formatVersion > PortableBackup.CURRENT_VERSION -> NEWER
                else -> formatVersion.toString()
            }
        reporter.recordException(BackupFailureEvent(), mapOf(KEY_FAILURE to failure.code, KEY_FORMAT to format, KEY_OP to op.code))
    }

    private suspend fun consented(): Boolean =
        try {
            settings.getSettings()?.isTelemetryEnabled ?: true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    companion object {
        const val KEY_FAILURE = "backup_failure_code"
        const val KEY_FORMAT = "backup_format_version"
        const val KEY_OP = "backup_op"
        private const val NEWER = "newer"
        private const val UNKNOWN = "unknown"
    }
}

/** The one non-fatal a failed backup or restore records; its message is fixed. */
class BackupFailureEvent : Exception("backup_failed")
