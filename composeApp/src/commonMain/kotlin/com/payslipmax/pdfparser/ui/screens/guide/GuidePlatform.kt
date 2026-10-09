package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.ui.platform.rememberClipboardCopier
import com.payslipmax.pdfparser.utils.shareText
import com.payslipmax.pdfparser.utils.shareTextViaEmail

/**
 * The three things the Guide hands to the device, each only ever called from a button tap: put text on the clipboard, open
 * the system share sheet, and open the user's mail app with a message ready (the app never sends it). A seam so tests can
 * see exactly what would leave, and when. This is the one Guide file that names the email helper; [email] defaults to a
 * no-op so a test that does not care cannot reach a real mail app.
 */
class GuidePlatform(
    val copy: (text: String) -> Unit,
    val share: (text: String, title: String) -> Unit,
    val email: (to: String, subject: String, body: String) -> Unit = { _, _, _ -> },
)

@Composable
internal fun rememberGuidePlatform(): GuidePlatform {
    val copy = rememberClipboardCopier()
    return remember(copy) { GuidePlatform(copy = copy, share = ::shareText, email = ::shareTextViaEmail) }
}
