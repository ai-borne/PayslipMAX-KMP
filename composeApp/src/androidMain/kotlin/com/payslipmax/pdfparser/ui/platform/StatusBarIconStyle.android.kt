package com.payslipmax.pdfparser.ui.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// targetSdk 36 forces edge-to-edge, so the status bar is transparent and its icon colour comes from the
// window's light-status-bar flag. The window theme (Theme.Material.NoActionBar) is dark, so without this
// the flag stays false: white icons on a light page.
@Composable
actual fun ApplyStatusBarIconStyle(style: StatusBarIconStyle) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = style.usesDarkIcons
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
