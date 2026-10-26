package com.payslipmax.pdfparser.ui.platform

import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler
import com.payslipmax.pdfparser.reminders.IosReminderScheduler

actual fun provideReminderScheduler(text: ReminderNotificationText): ReminderScheduler = IosReminderScheduler(text)
