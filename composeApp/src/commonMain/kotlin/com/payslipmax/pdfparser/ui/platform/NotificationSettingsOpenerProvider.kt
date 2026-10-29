package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.part2.reminders.NotificationSettingsOpener

/**
 * Opens the system screen where this app's notifications are turned on or off (Android needs a Context to start it, which only composition
 * has; on iOS it is the app's page in Settings, reached through the Swift gateway).
 */
@Composable
expect fun rememberNotificationSettingsOpener(): NotificationSettingsOpener
