package com.payslipmax.pdfparser.part2.reminders

/**
 * The words of a reminder notification, handed to the platform by injection: platform code owns no copy of its own. They are generic by
 * decision (DECISIONS Rounds 10 and 28): one sentence for every reminder, naming no code, order number or date and not even the kind,
 * so a lock screen shows nothing about the officer's leave or orders.
 */
data class ReminderNotificationText(
    val title: String,
    val body: String,
    val channelName: String,
    val channelDescription: String,
)
