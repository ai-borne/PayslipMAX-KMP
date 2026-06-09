package com.ssbmax.pdfparser

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.ssbmax.pdfparser.ui.PayslipViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private var filePickCallback: ((ByteArray, String) -> Unit)? = null

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
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
                }
            )
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
