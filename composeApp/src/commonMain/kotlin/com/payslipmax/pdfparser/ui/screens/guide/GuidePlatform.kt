package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.ui.platform.rememberClipboardCopier
import com.payslipmax.pdfparser.utils.shareText

/**
 * The two things the Guide hands to the device, both only ever called from a button tap: put text on the clipboard,
 * and open the system share sheet. A seam so tests can see exactly what would leave, and when.
 */
class GuidePlatform(
    val copy: (text: String) -> Unit,
    val share: (text: String, title: String) -> Unit,
)

@Composable
internal fun rememberGuidePlatform(): GuidePlatform {
    val copy = rememberClipboardCopier()
    return remember(copy) { GuidePlatform(copy = copy, share = ::shareText) }
}
