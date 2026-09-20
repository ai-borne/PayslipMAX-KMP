package com.payslipmax.pdfparser.utils

import com.payslipmax.pdfparser.crypto.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication

actual fun shareText(
    text: String,
    title: String,
) {
    presentIosShare(listOf(text))
}

private const val UNRESERVED_MARKS = "-._~"

internal fun percentEncodeUtf8(value: String): String =
    buildString {
        for (byte in value.encodeToByteArray()) {
            val code = byte.toInt() and 0xFF
            val char = code.toChar()
            if (code < 0x80 && (char.isLetterOrDigit() || char in UNRESERVED_MARKS)) {
                append(char)
            } else {
                append('%')
                append(code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }

internal fun buildMailtoUrlString(
    to: String,
    subject: String,
    body: String,
): String = "mailto:${percentEncodeUtf8(to)}?subject=${percentEncodeUtf8(subject)}&body=${percentEncodeUtf8(body)}"

actual fun shareTextViaEmail(
    to: String,
    subject: String,
    body: String,
) {
    val mailtoUrl = NSURL.URLWithString(buildMailtoUrlString(to, subject, body))
    if (mailtoUrl == null) {
        presentIosShare(listOf(body))
        return
    }
    UIApplication.sharedApplication.openURL(mailtoUrl, options = emptyMap<Any?, Any>()) { opened ->
        if (!opened) presentIosShare(listOf(body))
    }
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
