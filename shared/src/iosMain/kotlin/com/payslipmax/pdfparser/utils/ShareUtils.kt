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
internal fun prepareShareFile(
    bytes: ByteArray,
    fileName: String,
    baseDir: String = NSTemporaryDirectory(),
): NSURL {
    val cleanDir = baseDir.trimEnd('/')
    val path = "$cleanDir/$fileName"
    bytes.toNSData().writeToFile(path, atomically = true)
    return NSURL.fileURLWithPath(path)
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareBytes(
    bytes: ByteArray,
    fileName: String,
    mimeType: String,
) {
    val fileUrl = prepareShareFile(bytes, fileName)
    presentIosShare(listOf(fileUrl))
}
