package com.payslipmax.pdfparser.reminders

import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import kotlinx.coroutines.CompletableDeferred

/**
 * Android's notification permission (`POST_NOTIFICATIONS`, which exists from Android 13). [launch] shows the system dialog and the screen
 * that owns the launcher calls [onResult] with the answer. Before Android 13 nothing can be asked and notifications are on unless the
 * officer turned them off in settings ([areEnabled]); once granted there is nothing left to ask.
 */
class AndroidNotificationPermission(
    private val sdk: Int,
    private val isGranted: () -> Boolean,
    private val areEnabled: () -> Boolean,
    private val launch: () -> Unit,
) : NotificationPermission {
    private var answer: CompletableDeferred<Boolean>? = null

    override suspend fun canAsk(): Boolean = sdk >= PERMISSION_SDK && !isGranted()

    override suspend fun request(): Boolean {
        if (!canAsk()) return areEnabled()
        val waiting = CompletableDeferred<Boolean>()
        answer = waiting
        launch()
        return waiting.await()
    }

    override suspend fun isAllowed(): Boolean = (sdk < PERMISSION_SDK || isGranted()) && areEnabled()

    /** The system dialog's answer; ignored when nothing was asked. */
    fun onResult(granted: Boolean) {
        answer?.complete(granted)
    }

    private companion object {
        const val PERMISSION_SDK = 33
    }
}
