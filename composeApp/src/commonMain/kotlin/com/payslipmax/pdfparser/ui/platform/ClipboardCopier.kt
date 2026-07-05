package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable

// Compose Multiplatform has no common-code factory for building a `ClipEntry` from plain text
// (`ClipEntry`'s only public constructor today takes a platform-specific type, e.g. Android's
// `ClipData`), so each platform provides its own actual implementation.
@Composable
expect fun rememberClipboardCopier(): (String) -> Unit
