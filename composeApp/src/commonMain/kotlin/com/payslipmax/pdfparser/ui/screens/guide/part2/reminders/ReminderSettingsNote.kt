package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

/**
 * When the Due screen tells the officer that reminders can be turned on in the system settings (DECISIONS Round 44, plan item 39). Only after
 * the officer was asked (or the device had nothing to ask) and notifications are not allowed: before the first ask the explainer is still to
 * come, and once reminders are allowed there is nothing to say. It decides nothing about asking: a decline stays final (Round 28).
 */
object ReminderSettingsNote {
    fun isShown(
        asked: Boolean,
        allowed: Boolean,
    ): Boolean = asked && !allowed
}
