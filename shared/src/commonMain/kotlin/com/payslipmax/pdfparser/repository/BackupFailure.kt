package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import kotlinx.serialization.SerializationException

/**
 * Why a backup or restore failed, as the officer is told and as telemetry counts it (docs/Plan/14a P6, F5). [code] is the stable value
 * sent to Crashlytics and never the enum's `name`, so R8 or a rename cannot change what a dashboard counts.
 */
enum class BackupFailure(
    val code: String,
) {
    WRONG_PASSWORD("wrong_password"),
    NOT_A_BACKUP("not_a_backup"),
    NEWER_VERSION("newer_version"),
    DAMAGED("damaged"),
    TOO_LARGE("too_large"),
    STORAGE_FULL("storage_full"),
    VERIFY_FAILED("verify_failed"),
    WRITE_FAILED("write_failed"),
    UNKNOWN("unknown"),
    ;

    companion object {
        /** How deep a chain of causes is searched; a real one is two or three deep, and this stops a chain that loops. */
        private const val CAUSE_DEPTH_MAX = 8

        /**
         * The failure [error] stands for (the one mapper, SSOT for the screen and telemetry): decided by its type, or by the type of the
         * first cause that has one, never by any message. Anything else, or no error at all, is [UNKNOWN].
         */
        fun of(error: Throwable?): BackupFailure {
            var current = error
            var depth = 0
            while (current != null && depth++ < CAUSE_DEPTH_MAX) {
                byType(current)?.let { return it }
                current = current.cause
            }
            return UNKNOWN
        }

        private fun byType(error: Throwable): BackupFailure? =
            when (error) {
                is DecryptionFailedException -> WRONG_PASSWORD
                is NotABackupException -> NOT_A_BACKUP
                is UnsupportedBackupVersionException -> NEWER_VERSION
                // kotlinx's own type: a document that opened but does not decode. Its message quotes the JSON, and is never read.
                is BackupDamagedException, is SerializationException -> DAMAGED
                is BackupTooLargeException -> TOO_LARGE
                is BackupStorageFullException -> STORAGE_FULL
                is BackupVerifyException -> VERIFY_FAILED
                is BackupFileException -> WRITE_FAILED
                else -> null
            }
    }
}

/** Which operation failed, for telemetry. [code] is stable, never `name`. */
enum class BackupOp(
    val code: String,
) {
    BACKUP("backup"),
    RESTORE("restore"),
}

/** A file without a header or page archive that is too short to hold even one sealed row: it cannot be a backup. Fixed message. */
class NotABackupException : Exception("This file is not a PayslipMax backup.")

/** A file without a page archive larger than any backup without pages can be: it is not read. Fixed message. */
class BackupTooLargeException : Exception("This file is too large to restore.")

/** The device has not got the free space a backup or restore needs; checked before anything is written. Fixed message. */
class BackupStorageFullException : Exception("There is not enough free space on this device.")
