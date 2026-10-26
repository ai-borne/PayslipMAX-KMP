package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.reminders.NotificationPermission

/** A [NotificationPermission] with fixed answers that counts how often the system dialog would have been shown. */
class FakeNotificationPermission(
    var askable: Boolean = true,
    var grants: Boolean = true,
) : NotificationPermission {
    var requests = 0
        private set

    override suspend fun canAsk(): Boolean = askable

    override suspend fun request(): Boolean {
        requests++
        askable = false
        return grants
    }
}
