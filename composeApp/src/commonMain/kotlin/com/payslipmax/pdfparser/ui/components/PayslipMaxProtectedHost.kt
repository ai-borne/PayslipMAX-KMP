package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Host container composable that gates visibility of its content behind a privacy shield overlay.
 *
 * When the app is in the active foreground ([Lifecycle.State.RESUMED]), the underlying content
 * is rendered normally and in-app screenshots remain functional.
 *
 * When the app transitions away from the foreground (Recent Apps / App Switcher, backgrounding,
 * or cold launch pre-resume), [PayslipMaxProtectedOverlay] is displayed over the content to
 * prevent sensitive financial PII from leaking into OS task switcher thumbnails or window previews.
 */
@Composable
fun PayslipMaxProtectedHost(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    content: @Composable () -> Unit,
) {
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val isResumed = lifecycleState == Lifecycle.State.RESUMED

    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (!isResumed) {
            PayslipMaxProtectedOverlay()
        }
    }
}
