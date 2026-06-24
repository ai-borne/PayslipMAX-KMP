package com.ssbmax.pdfparser.utils

import android.content.Intent
import com.ssbmax.pdfparser.crypto.ContextHolder

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
