package com.payslipmax.pdfparser.utils

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import java.io.File

actual fun shareTextViaEmail(
    to: String,
    subject: String,
    body: String,
) {
    val context = ContextHolder.context ?: return
    val emailIntent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    if (emailIntent.resolveActivity(context.packageManager) != null) {
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(emailIntent)
    } else {
        shareText(body, subject)
    }
}

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

/** Offers [file] (in the app's cache) to the share sheet by a FileProvider URI; the app never uploads it itself. */
fun shareBackupFile(file: File) {
    val context = ContextHolder.context ?: throw IllegalStateException("No context")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooser =
        Intent.createChooser(send, file.name).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(chooser)
}
