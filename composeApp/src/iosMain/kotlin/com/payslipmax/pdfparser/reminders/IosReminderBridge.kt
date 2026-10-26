package com.payslipmax.pdfparser.reminders

import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * One reminder as the Swift side receives it: [key] names the system request (so it is replaced, not doubled), [kind] and [draftId] ride in
 * its user info and are all a tap can carry back, [fireAtMillis] is the planner's instant, and [title] and [body] are the injected words. No
 * date arithmetic happens in Swift beyond turning that instant into "seconds from now". [toString] hides the draft id.
 */
class IosReminderRequest(
    val key: String,
    val kind: String,
    val draftId: String,
    val fireAtMillis: Long,
    val title: String,
    val body: String,
) {
    override fun toString(): String = "IosReminderRequest($kind)"
}

/**
 * The Swift side of reminders (`ReminderNotificationGateway` in iosApp, over `UNUserNotificationCenter`), set once at app start. Kotlin/Native
 * reaches the notification center only through it, so the center, its delegate and its permission prompt stay in Swift where XCTest can
 * reach them. All callbacks arrive on the main thread.
 */
interface IosNotificationGateway {
    /** Makes the system's pending reminders exactly [requests], replacing by key; an empty list removes them all. */
    fun replaceAll(requests: List<IosReminderRequest>)

    /** Answers whether the system would still show its permission dialog (the authorization is not decided yet). */
    fun canAsk(listener: IosPermissionListener)

    /** Shows the system's permission dialog and answers whether notifications are allowed afterwards. */
    fun request(listener: IosPermissionListener)
}

/** The Swift gateway's answer to a permission question; called once, on the main thread. A second call is ignored. */
interface IosPermissionListener {
    fun onAnswer(allowed: Boolean)
}

/** Where Swift registers its [IosNotificationGateway] at startup (`ReminderNotificationGateway.register()`). */
class IosReminderBridge {
    companion object {
        var gateway: IosNotificationGateway? = null
    }
}

/**
 * The iOS [ReminderScheduler]: turns the planner's `upcoming` into requests for the Swift gateway, with the injected [text]. The system keeps
 * at most [OS_PENDING_LIMIT] pending requests, and the planner never gives more than `ReminderPolicy.upcomingMax`, so none is dropped.
 * With no gateway registered (a unit test, or a build that never registered one) it does nothing.
 */
class IosReminderScheduler(
    private val text: ReminderNotificationText,
    private val gateway: () -> IosNotificationGateway? = { IosReminderBridge.gateway },
) : ReminderScheduler {
    override fun replaceAll(reminders: List<Reminder>) {
        gateway()?.replaceAll(reminders.map { IosReminderRequest(it.key, it.kind.name, it.draftId, it.at, text.title, text.body) })
    }

    companion object {
        /** UNUserNotificationCenter holds at most 64 pending local notifications; later ones are silently discarded. */
        const val OS_PENDING_LIMIT = 64
    }
}

/** The iOS [NotificationPermission]: the system's authorization request, asked through the Swift gateway. */
class IosNotificationPermission(
    private val gateway: () -> IosNotificationGateway? = { IosReminderBridge.gateway },
) : NotificationPermission {
    override suspend fun canAsk(): Boolean = ask { listener -> gateway()?.canAsk(listener) ?: listener.onAnswer(false) }

    override suspend fun request(): Boolean = ask { listener -> gateway()?.request(listener) ?: listener.onAnswer(false) }

    private suspend fun ask(start: (IosPermissionListener) -> Unit): Boolean =
        suspendCancellableCoroutine { continuation ->
            start(
                object : IosPermissionListener {
                    override fun onAnswer(allowed: Boolean) {
                        if (continuation.isActive) continuation.resume(allowed)
                    }
                },
            )
        }
}
