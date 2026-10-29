package com.payslipmax.pdfparser.part2.reminders

/**
 * Opens the system screen where the officer can turn this app's notifications back on (Android's notification settings for the app; iOS's
 * settings page for the app). The only platform part of the "reminders are off" note (DECISIONS Round 44): whether the note shows is common code.
 */
fun interface NotificationSettingsOpener {
    fun open()
}
