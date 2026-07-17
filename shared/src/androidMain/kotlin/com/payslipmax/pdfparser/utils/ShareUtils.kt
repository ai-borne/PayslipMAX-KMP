package com.payslipmax.pdfparser.utils

import android.content.Intent
import androidx.core.content.FileProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import java.io.File

actual fun shareText(
    text: String,
    title: String,
) {
    val context = ContextHolder.context ?: return
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    val chooser =
        Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(chooser)
}

actual fun shareBytes(
    bytes: ByteArray,
    fileName: String,
    mimeType: String,
) {
    val context = ContextHolder.context ?: return
    val file = File(context.cacheDir, fileName)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooser =
        Intent.createChooser(send, fileName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(chooser)
}
