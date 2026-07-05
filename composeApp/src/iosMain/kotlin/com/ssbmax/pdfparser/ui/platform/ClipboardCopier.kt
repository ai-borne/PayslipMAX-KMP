package com.ssbmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberClipboardCopier(): (String) -> Unit {
    return { text -> UIPasteboard.generalPasteboard.string = text }
}
