package com.payslipmax.pdfparser.part2.reminders

/**
 * The platform's notification permission. On Android it is built from the screen (the system dialog needs an Activity); on iOS it is the
 * system's authorization request. Both answer without ever showing anything of the officer's.
 */
interface NotificationPermission {
    /** True when the system would still show its own dialog: the permission exists on this device and has not been decided. */
    suspend fun canAsk(): Boolean

    /** Shows the system dialog and answers whether notifications are now allowed. */
    suspend fun request(): Boolean

    /** True when a reminder could show right now: the permission is granted and the officer has not turned notifications off. Shows nothing. */
    suspend fun isAllowed(): Boolean
}
