package com.payslipmax.pdfparser.utils

import com.payslipmax.pdfparser.crypto.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToFile

actual fun shareText(
    text: String,
    title: String,
) {
    presentIosShare(listOf(text))
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareBytes(
    bytes: ByteArray,
    fileName: String,
    mimeType: String,
) {
    val path = NSTemporaryDirectory() + fileName
    bytes.toNSData().writeToFile(path, atomically = true)
    presentIosShare(listOf(NSURL.fileURLWithPath(path)))
}
