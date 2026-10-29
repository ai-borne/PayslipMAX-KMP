package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.part2.reminders.NotificationSettingsOpener
import com.payslipmax.pdfparser.reminders.IosNotificationSettingsOpener

@Composable
actual fun rememberNotificationSettingsOpener(): NotificationSettingsOpener = remember { IosNotificationSettingsOpener() }
