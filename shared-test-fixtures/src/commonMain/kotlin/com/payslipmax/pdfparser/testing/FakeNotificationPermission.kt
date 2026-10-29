package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.reminders.NotificationPermission

/** A [NotificationPermission] with fixed answers that counts how often the system dialog would have been shown; [allowed] is what a reminder could do now and follows the officer's answer. */
class FakeNotificationPermission(
    var askable: Boolean = true,
    var grants: Boolean = true,
    var allowed: Boolean = false,
) : NotificationPermission {
    var requests = 0
        private set

    override suspend fun canAsk(): Boolean = askable

    override suspend fun request(): Boolean {
        requests++
        askable = false
        allowed = grants
        return grants
    }

    override suspend fun isAllowed(): Boolean = allowed
}
