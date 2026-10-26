package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.reminders.ReminderPromptStore

/** In-memory [ReminderPromptStore]; a fresh device has not been asked. */
class FakeReminderPromptStore(
    private var asked: Boolean = false,
) : ReminderPromptStore {
    override fun wasAsked(): Boolean = asked

    override fun markAsked() {
        asked = true
    }
}
