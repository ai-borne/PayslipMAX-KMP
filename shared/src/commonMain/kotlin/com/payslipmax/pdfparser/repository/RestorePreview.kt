package com.payslipmax.pdfparser.repository

/**
 * The rows a restore leaves out, per kind (docs/Plan/14a P5, F6; the one type for it, SSOT): a row that will not open is skipped and
 * counted, never fatal. Only kinds that are opened one row at a time can be skipped: PDFs, letters, deleted-letter records and settings are
 * plain JSON inside the authenticated document, so they come back whole once it opens. Drafts from the frozen Part II branch are never
 * restored and count as [part2Entries], the kind that replaced them. Counts only, never content.
 */
data class SkippedRows(
    val payslips: Int = 0,
    val corrections: Int = 0,
    val guideNotes: Int = 0,
    val part2Entries: Int = 0,
    val part2Orders: Int = 0,
    val part2Pages: Int = 0,
) {
    /** Every row left out, for the one status line that shows a single number. */
    val total: Int get() = payslips + corrections + guideNotes + part2Entries + part2Orders + part2Pages

    companion object {
        val NONE = SkippedRows()
    }
}

/**
 * What a backup holds, read and checked without writing anything (docs/Plan/14a P4): the counts a restore would write, the rows it would
 * leave out ([skipped], per kind), the file's format and, from format 6, when it was made ([exportedAt], epoch millis; null for older files).
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
    val skipped: SkippedRows,
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
