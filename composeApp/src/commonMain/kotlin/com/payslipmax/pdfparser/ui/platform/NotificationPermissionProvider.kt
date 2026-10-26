package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.part2.reminders.NotificationPermission

/**
 * The platform's notification permission, built from the screen the officer is looking at (Android's system dialog needs an Activity's
 * result launcher, which only composition has). On iOS it is the system's authorization request.
 */
@Composable
expect fun rememberNotificationPermission(): NotificationPermission
