package com.payslipmax.pdfparser.repository

/**
 * The one private file a backup passes through on its way to the share sheet, and the one a picked backup is copied into on its way in.
 * Nothing else is written: the names are fixed, no path or name is logged or shown, and a failure carries only a fixed message
 * ([BackupFileException]). The ViewModel talks to this interface so tests swap in a fake.
 */
interface BackupFiles {
    /** Starts the export file afresh, lets [block] write the archive into it, and deletes the file if [block] fails or throws. */
    suspend fun write(block: suspend (BackupSink) -> Result<Unit>): Result<Unit>

    /** Hands the written export file to the platform share sheet; deletes it if the sheet cannot be opened. */
    fun share(): Result<Unit>

    /** Reads the [picked] backup file by range through [block], and always deletes it afterwards, whatever [block] returns. */
    suspend fun <R> read(
        picked: String,
        block: suspend (BackupSource) -> Result<R>,
    ): Result<R>

    /** Deletes a [picked] file that will not be restored (the officer dismissed the Replace or Merge question). */
    fun discard(picked: String)

    /** Deletes every backup file this app may have left in its private cache. */
    fun sweep()
}

/** The one name the export file has on every platform; it is also the name the share target sees. */
const val BACKUP_EXPORT_FILE_NAME = "PayslipMax-backup.pcda"

/** Any trouble with the backup file itself; the cause is dropped on purpose because an I/O message can carry a path. */
class BackupFileException : Exception("The backup file could not be used.")

/** The platform's backup files (private cache on Android, the temporary directory on iOS). */
expect fun provideBackupFiles(): BackupFiles
