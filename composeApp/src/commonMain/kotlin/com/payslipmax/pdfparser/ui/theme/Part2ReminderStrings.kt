package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText

/**
 * Copy for the Part II reminder notifications and for the one-time ask before the system permission dialog (docs/Plan/12_Part2Orders_PhasePlan.md,
 * Phase 7b; DECISIONS Round 28). The notification text is one fixed, generic sentence: it names no code, order number or date, not even the
 * kind of reminder, because it can show on a lock screen. The Due list gives the detail once the officer opens the app.
 */
object Part2ReminderStrings {
    // The notification and its channel (the channel name is what the system's notification settings list).
    const val notificationTitle = "PayslipMax"
    const val notificationBody = "A Part II reminder is due."
    const val channelName = "Part II reminders"
    const val channelDescription = "Reminders from your printed Part II slips. They never say what is due."

    fun notificationText() = ReminderNotificationText(notificationTitle, notificationBody, channelName, channelDescription)

    // The explainer shown once, before the system's own dialog.
    const val promptTitle = "Turn on reminders?"
    const val promptBody =
        "PayslipMax can remind you when a printed slip needs something from you, for example to add the published order. " +
            "A reminder only says that something is due: it never names a code, an order number or a date. Reminders stay on this device."
    const val promptAllow = "Allow reminders"
    const val promptNotNow = "Not now"

    // The note on the Due screen once reminders are off (Round 44, plan item 39): the way back after a decline.
    const val settingsNote = "Reminders are off. You can turn them on in your phone's notification settings."
    const val settingsOpen = "Open settings"

    // The debug-only switch in Settings (debug builds only).
    const val debugDelayTitle = "Bring Part II reminders forward"
    const val debugDelayDesc = "Reminders fall 20 seconds from now instead of on their dates. Debug builds only; resets on every launch."
}
