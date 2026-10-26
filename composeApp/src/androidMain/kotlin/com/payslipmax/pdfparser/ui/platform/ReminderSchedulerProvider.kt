package com.payslipmax.pdfparser.ui.platform

import androidx.work.WorkManager
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler
import com.payslipmax.pdfparser.reminders.AndroidReminderScheduler

actual fun provideReminderScheduler(text: ReminderNotificationText): ReminderScheduler = AndroidReminderScheduler(workManager = { WorkManager.getInstance(checkNotNull(ContextHolder.context)) })
