package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.utils.presentIosShare
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL

/**
 * The export file in the temporary directory under one fixed name. iOS tells us when the share sheet is done, so the file is deleted
 * then; a failure removes it at once and [sweep] clears what a killed process left. A picked backup arrives as a private copy the
 * document picker made (the Swift side asks for `asCopy`) and is deleted after the restore or when dismissed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBackupFiles(
    private val tempDir: String = NSTemporaryDirectory(),
    private val share: (path: String, onDone: () -> Unit) -> Unit = { path, onDone ->
        presentIosShare(listOf(NSURL.fileURLWithPath(path)), onDone)
    },
) : FileBackupFiles() {
    override val exportPath: String get() = "${tempDir.trimEnd('/')}/$BACKUP_EXPORT_FILE_NAME"

    override fun leftovers(): List<String> = listOf(exportPath)

    override fun openSink(path: String): ClosableBackupSink = FileBackupSink(path)

    override fun openSource(path: String): ClosableBackupSource = FileBackupSource(path)

    override fun delete(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }

    override fun present(path: String) = share(path) { discard(path) }
}
