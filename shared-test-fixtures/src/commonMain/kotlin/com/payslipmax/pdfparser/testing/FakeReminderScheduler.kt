package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler

/** A [ReminderScheduler] that records every list it is given: no operating system. [failNext] makes the next call throw, as a full device can. */
class FakeReminderScheduler : ReminderScheduler {
    val calls = mutableListOf<List<Reminder>>()
    var failNext = false

    /** What the system would now hold: the last list given, or nothing before the first call. */
    val pending: List<Reminder> get() = calls.lastOrNull().orEmpty()

    override fun replaceAll(reminders: List<Reminder>) {
        if (failNext) {
            failNext = false
            error("the scheduler failed")
        }
        calls += reminders
    }
}
