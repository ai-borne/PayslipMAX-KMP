package com.payslipmax.pdfparser

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.ssbmax.pdfparser.App
import com.ssbmax.pdfparser.ui.PayslipViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private var filePickCallback: ((ByteArray, String) -> Unit)? = null

    private val pickPdfLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                val bytes = readBytes(uri)
                val filename = getFileName(uri) ?: "payslip.pdf"
                if (bytes != null) {
                    filePickCallback?.invoke(bytes, filename)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: PayslipViewModel = koinInject()
            App(
                viewModel = viewModel,
                onPickPdf = { callback ->
                    filePickCallback = callback
                    pickPdfLauncher.launch("application/pdf")
                },
                onOpenPdf = { bytes, filename ->
                    openPdf(bytes, filename)
                },
            )
        }
    }

    private fun openPdf(
        bytes: ByteArray,
        filename: String,
    ) {
        try {
            val cacheFile = java.io.File(cacheDir, filename)
            cacheFile.writeBytes(bytes)
            val uri =
                androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    cacheFile,
                )
            val intent =
                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_ACTIVITY_NO_HISTORY or
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
            startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readBytes(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/')
    }
}
