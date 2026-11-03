package com.payslipmax.pdfparser.repository

/**
 * What a backup holds, read and checked without writing anything (docs/Plan/14a P4): the counts a restore would write, the Part II rows it
 * would leave out ([skipped]), the file's format and, from format 6, when it was made ([exportedAt], epoch millis; null for older files).
 */
data class RestorePreview(
    val formatVersion: Int,
    val exportedAt: Long?,
    val payslips: Int,
    val pdfs: Int,
    val corrections: Int,
    val guideNotes: Int,
    val drafts: Int,
    val dismissedDrafts: Int,
    val part2Entries: Int,
    val part2Orders: Int,
    val part2Pages: Int,
    val skipped: Int,
)

/** Where a backup or restore is. Each stage's `total` is 0 while it is not known yet (a spinner, then a bar). */
enum class BackupStage {
    /** The password is turned into the archive's key (the slow PBKDF2 step), or the file is opened. */
    OPENING,

    /** Payslips and the other rows are read and re-sealed. */
    READING,

    /** Scanned pages are written into the file, or into the database. */
    PAGES,

    /** A just-written backup is read back and checked before it is shared. */
    VERIFYING,
}

/** Progress of a backup or restore: called on the worker thread, often; the receiver throttles. Counts only, never content. */
fun interface BackupProgress {
    fun update(
        stage: BackupStage,
        done: Int,
        total: Int,
    )

    companion object {
        val NONE = BackupProgress { _, _, _ -> }
    }
}

/** A backup that was written but did not read back: the file is deleted and never shared. Fixed message: no path, no content. */
class BackupVerifyException : Exception("The backup could not be checked after it was written, so it was not kept.")
