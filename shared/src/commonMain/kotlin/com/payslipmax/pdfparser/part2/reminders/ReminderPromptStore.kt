package com.payslipmax.pdfparser.part2.reminders

/**
 * Whether the officer has already been asked to allow reminders on THIS device (DECISIONS Round 28: a device-local preference, not Room and
 * not the `.pcda` backup, because the permission is the device's). Once marked it stays marked, so a decline is never asked about again.
 */
interface ReminderPromptStore {
    fun wasAsked(): Boolean

    fun markAsked()
}

expect fun provideReminderPromptStore(): ReminderPromptStore
