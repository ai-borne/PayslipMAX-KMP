package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.utils.shareBackupFile
import java.io.File
import java.io.InputStream

private const val RESTORE_NAME = "PayslipMax-restore.pcda"

/**
 * The backup files in the app's private cache, under two fixed names. The share sheet cannot say when the target app has finished
 * reading, so a shared export stays until the next launch or the next backup sweeps it ([sweep]); a failure removes it at once.
 */
class AndroidBackupFiles(
    private val cacheDir: () -> File? = { ContextHolder.context?.cacheDir },
    private val share: (File) -> Unit = ::shareBackupFile,
) : FileBackupFiles() {
    override val exportPath: String get() = File(requireNotNull(cacheDir()), BACKUP_EXPORT_FILE_NAME).path

    override fun leftovers(): List<String> = cacheDir()?.let { listOf(File(it, BACKUP_EXPORT_FILE_NAME).path, File(it, RESTORE_NAME).path) }.orEmpty()

    override fun openSink(path: String): ClosableBackupSink = FileBackupSink(File(path))

    override fun openSource(path: String): ClosableBackupSource = FileBackupSource(File(path))

    override fun delete(path: String) {
        File(path).delete()
    }

    override fun present(path: String) = share(File(path))
}

/**
 * Copies the backup the officer picked into the private restore file, by stream and never as one array, starting the file afresh, and
 * returns its path for [BackupFiles.read]. A copy that fails leaves no file behind.
 */
fun copyPickedBackup(
    cacheDir: File,
    input: InputStream,
): String {
    val target = File(cacheDir, RESTORE_NAME)
    try {
        target.outputStream().use { input.copyTo(it) }
    } catch (e: Exception) {
        target.delete()
        throw BackupFileException()
    }
    return target.path
}
