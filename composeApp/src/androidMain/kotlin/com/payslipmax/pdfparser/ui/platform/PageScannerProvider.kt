package com.payslipmax.pdfparser.ui.platform

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.payslipmax.pdfparser.part2.pages.AndroidPageScanner
import com.payslipmax.pdfparser.part2.pages.MlKitScanSource
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.part2.pages.ScanActivityResult
import com.payslipmax.pdfparser.part2.pages.ScanResultBridge
import java.io.File

@Composable
actual fun rememberPageScanner(): PageScanner {
    val context = LocalContext.current
    val activity = context.findActivity()
    val bridge = remember { ScanResultBridge() }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            bridge.onResult(ScanActivityResult(result.resultCode == Activity.RESULT_OK, result.data))
        }
    return remember(activity, launcher) {
        bridge.start = { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
        AndroidPageScanner(File(context.cacheDir, MlKitScanSource.OUTPUT_DIR), MlKitScanSource(activity, bridge))
    }
}
