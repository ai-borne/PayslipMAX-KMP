package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.repository.BackupFailure

/**
 * What a failed backup or restore says: one fixed line per [BackupFailure] (docs/Plan/14a P6). The line is chosen by the failure's type,
 * so no exception text, path, file name or row content is ever shown. New lines are reviewed by the owner in the P8 walk (D13).
 */
object BackupRestoreStrings {
    const val wrongPasswordOrNotABackup = "Wrong password, or this isn't a PayslipMax backup"
    const val notABackup = "This file isn't a PayslipMax backup. Pick the file that Back Up made."
    const val newerVersion = "This backup was made by a newer version of PayslipMax. Update the app, then try again."
    const val damaged = "This backup file is damaged and cannot be restored."
    const val tooLarge = "This file is too large to be a PayslipMax backup."
    const val storageFull = "There isn't enough free space on this device. Free some space, then try again."
    const val verifyFailed = "The backup could not be checked after it was written, so it was not kept. Try again."
    const val writeFailed = "The backup file could not be written or read on this device. Try again."
    const val failedGeneric = "Something went wrong. Try again."

    /** The one line for [failure]. */
    fun failure(failure: BackupFailure): String =
        when (failure) {
            BackupFailure.WRONG_PASSWORD -> wrongPasswordOrNotABackup
            BackupFailure.NOT_A_BACKUP -> notABackup
            BackupFailure.NEWER_VERSION -> newerVersion
            BackupFailure.DAMAGED -> damaged
            BackupFailure.TOO_LARGE -> tooLarge
            BackupFailure.STORAGE_FULL -> storageFull
            BackupFailure.VERIFY_FAILED -> verifyFailed
            BackupFailure.WRITE_FAILED -> writeFailed
            BackupFailure.UNKNOWN -> failedGeneric
        }
}
