package com.payslipmax.pdfparser.ui.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.payslipmax.pdfparser.part2.reminders.NotificationSettingsOpener
import com.payslipmax.pdfparser.reminders.AndroidNotificationSettingsOpener

@Composable
actual fun rememberNotificationSettingsOpener(): NotificationSettingsOpener {
    val context = LocalContext.current
    return remember(context) { AndroidNotificationSettingsOpener(context.packageName, context is Activity) { context.startActivity(it) } }
}
