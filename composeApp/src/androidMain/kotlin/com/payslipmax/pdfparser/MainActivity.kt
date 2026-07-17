package com.payslipmax.pdfparser

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.payslipmax.pdfparser.insights.gemma.AndroidGemmaBaseModelInstaller
import com.payslipmax.pdfparser.ui.PayslipViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private var filePickCallback: ((ByteArray, String) -> Unit)? = null
    private var backupPickCallback: ((ByteArray) -> Unit)? = null

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

    // Backup archives (.pcda) aren't a recognized document type, so the picker opens on "*/*"
    // rather than a MIME filter that would hide them.
    private val pickBackupLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                val bytes = readBytes(uri)
                if (bytes != null) {
                    backupPickCallback?.invoke(bytes)
                }
            }
        }

    // Play Asset Delivery's cellular-consent / unrecognized-app confirmation shows an IntentSender
    // the OS can only launch from an Activity — AndroidGemmaBaseModelInstaller lives outside any
    // Activity, so it calls back into this launcher via the confirmationHandler bridge instead. The
    // result itself needs no handling here: the next AssetPackStateUpdateListener callback reports
    // whatever the user decided.
    private val gemmaConfirmationLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidGemmaBaseModelInstaller.confirmationHandler = { assetPackManager ->
            assetPackManager.showConfirmationDialog(gemmaConfirmationLauncher)
        }
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
                onPickBackup = { callback ->
                    backupPickCallback = callback
                    pickBackupLauncher.launch("*/*")
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
