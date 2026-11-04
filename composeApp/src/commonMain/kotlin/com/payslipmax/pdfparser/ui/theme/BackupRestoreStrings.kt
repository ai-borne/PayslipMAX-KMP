package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.RestoredRows
import com.payslipmax.pdfparser.repository.SkippedRows

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

    // ---- The sheet (docs/Plan/14a P8). New copy, reviewed by the owner in the P8 walk (D13).

    /** What a screen reader reads in place of a typed password (P10): one bullet per character, never the characters. */
    const val maskCharacter = '\u2022'
    const val confirmLabel = "Confirm Password"
    const val confirmHint = "Re-enter the password to Back Up. Not needed to Restore."
    const val passwordMismatch = "The passwords don't match."
    const val lastBackupNever = "No backup has been made on this device yet."
    const val previewDate = "Made on"
    const val restoreBtn = "Restore"
    const val cancelBtn = "Cancel"
    const val replaceSkipsTitle = "Replace and lose these rows?"
    const val replaceAnywayBtn = "Replace anyway"
    const val goBackBtn = "Go back"

    fun lastBackup(date: String): String = "Last backup: $date"

    fun previewMadeOn(date: String): String = "$previewDate $date"

    fun previewFormat(version: Int): String = "Backup format $version"

    /** One fixed phrase per stage; a number only for pages, where there is something to count. */
    fun progressLine(
        op: BackupOp,
        stage: BackupStage,
        done: Int,
        total: Int,
    ): String {
        val backup = op == BackupOp.BACKUP
        return when (stage) {
            BackupStage.OPENING -> if (backup) "Securing your backup…" else "Opening the backup…"
            BackupStage.READING -> if (backup) "Encrypting your data…" else "Checking the backup…"
            BackupStage.VERIFYING -> "Checking the backup file…"
            BackupStage.PAGES ->
                when {
                    total <= 0 -> if (backup) "Backing up…" else "Restoring…"
                    else -> "${if (backup) "Backing up" else "Restoring"} pages: $done of $total"
                }
        }
    }

    /** What the backup holds, never what a restore will add: a Merge may change less than this. */
    fun previewHolds(preview: RestorePreview): String {
        val parts =
            listOfNotNull(
                count(preview.payslips, "payslip", "payslips"),
                count(preview.part2Entries, "Orders Bank entry", "Orders Bank entries"),
                count(preview.part2Orders, "order", "orders"),
                count(preview.part2Pages, "page", "pages"),
            )
        return if (parts.isEmpty()) "This backup holds no payslips or Orders Bank items." else "This backup holds ${parts.joinToString(", ")}."
    }

    /** The skipped rows per kind, kinds with none left out: "2 payslips, 1 note". Empty when nothing was skipped. */
    fun skippedList(skipped: SkippedRows): String =
        listOfNotNull(
            count(skipped.payslips, "payslip", "payslips"),
            count(skipped.corrections, "correction", "corrections"),
            count(skipped.guideNotes, "note", "notes"),
            count(skipped.part2Entries, "Orders Bank entry", "Orders Bank entries"),
            count(skipped.part2Orders, "order", "orders"),
            count(skipped.part2Pages, "page", "pages"),
        ).joinToString(", ")

    fun previewSkipped(skipped: SkippedRows): String = "Could not be read, so not restored: ${skippedList(skipped)}."

    /** D11: what Replace costs, named, before the officer confirms it a second time. */
    fun replaceSkipsMessage(skipped: SkippedRows): String =
        "Replace deletes everything on this device and restores only what could be read. This will be lost: ${skippedList(skipped)}."

    fun restoredSkipped(skipped: SkippedRows): String = "Not restored: ${skippedList(skipped)}."

    /** What a finished restore wrote, kinds with none left out ("Restored 34 payslips, 1 order, 1 page."), or null when it wrote none of them. */
    fun restoredCounts(restored: RestoredRows): String? =
        listOfNotNull(
            count(restored.payslips, "payslip", "payslips"),
            count(restored.part2Entries, "Orders Bank entry", "Orders Bank entries"),
            count(restored.part2Orders, "order", "orders"),
            count(restored.part2Pages, "page", "pages"),
        ).takeIf { it.isNotEmpty() }?.let { "Restored ${it.joinToString(", ")}." }

    private fun count(
        n: Int,
        one: String,
        many: String,
    ): String? = if (n <= 0) null else "$n ${if (n == 1) one else many}"
}
