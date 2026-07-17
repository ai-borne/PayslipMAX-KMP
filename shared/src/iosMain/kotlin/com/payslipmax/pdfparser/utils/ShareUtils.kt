package com.payslipmax.pdfparser.utils

import com.payslipmax.pdfparser.crypto.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

actual fun shareText(
    text: String,
    title: String,
) {
    presentActivity(listOf(text))
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareBytes(
    bytes: ByteArray,
    fileName: String,
    mimeType: String,
) {
    val path = NSTemporaryDirectory() + fileName
    bytes.toNSData().writeToFile(path, atomically = true)
    presentActivity(listOf(NSURL.fileURLWithPath(path)))
}

private fun presentActivity(items: List<Any>) {
    val keyWindow =
        UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = keyWindow?.rootViewController ?: return

    val activityController = UIActivityViewController(items, null)
    rootViewController.presentViewController(activityController, animated = true, completion = null)
}
