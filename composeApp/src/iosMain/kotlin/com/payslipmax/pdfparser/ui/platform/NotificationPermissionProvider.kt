package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import com.payslipmax.pdfparser.reminders.IosNotificationPermission

@Composable
actual fun rememberNotificationPermission(): NotificationPermission = remember { IosNotificationPermission() }
