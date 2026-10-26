package com.payslipmax.pdfparser.ui.platform

import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler

/**
 * The platform's scheduler for Part II reminders: WorkManager on Android, `UNUserNotificationCenter` on iOS. Built without touching the
 * platform. [text] is the injected notification wording: iOS puts it in each request; on Android the worker reads it from Koin when it posts.
 */
expect fun provideReminderScheduler(text: ReminderNotificationText): ReminderScheduler
