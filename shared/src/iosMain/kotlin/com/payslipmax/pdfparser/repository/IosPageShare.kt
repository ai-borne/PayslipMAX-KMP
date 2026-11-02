package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.utils.presentIosShare
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** The shared page in the temporary directory under one fixed name; iOS says when the share sheet closes, and the file is deleted then. */
@OptIn(ExperimentalForeignApi::class)
class IosPageShare(
    private val tempDir: String = NSTemporaryDirectory(),
) : FilePageShare() {
    override val path: String get() = "${tempDir.trimEnd('/')}/$PAGE_SHARE_FILE_NAME"

    override fun write(jpeg: ByteArray) = FileBackupSink(path).use { it.write(jpeg) }

    // UIKit presents on the main queue only; the page was read on a background one.
    override fun present(path: String) {
        dispatch_async(dispatch_get_main_queue()) { presentIosShare(listOf(NSURL.fileURLWithPath(path))) { delete(path) } }
    }

    override fun delete(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}

actual fun providePageShare(): PageShare = IosPageShare()
