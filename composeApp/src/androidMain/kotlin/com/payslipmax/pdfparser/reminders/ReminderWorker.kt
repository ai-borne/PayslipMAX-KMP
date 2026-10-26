package com.payslipmax.pdfparser.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.payslipmax.pdfparser.nav.Part2Link
import com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck
import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler
import kotlinx.coroutines.CancellationException
import org.koin.core.context.GlobalContext

/**
 * Runs when the system says a reminder's time has come, possibly in a process the officer never opened. The input is a reminder kind and a
 * draft id (nothing else), read as untrusted text. The worker asks the drafts, now, whether that reminder is still due ([ReminderFireCheck]),
 * and posts the generic notification only if it is. With Part II off the Koin bindings are absent and it does nothing. It always ends in
 * success: a reminder that cannot be posted is simply not posted, never retried in a loop, and nothing about it is logged.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val koin = GlobalContext.getOrNull() ?: return Result.success()
        val check = koin.getOrNull<ReminderFireCheck>() ?: return Result.success()
        val text = koin.getOrNull<ReminderNotificationText>() ?: return Result.success()
        val link = Part2Link.parse(inputData.getString(KIND), inputData.getString(DRAFT)) ?: return Result.success()
        try {
            // The debug walk switch only moves a reminder in time, so while it is on (never outside a debug build) only the draft is checked.
            val walking = koin.getOrNull<ShortDelayReminderScheduler>()?.isShort == true
            val post = if (walking) check.draftExists(link.draftId) else check.shouldPost(link.kind, link.draftId)
            if (post) ReminderNotifier(applicationContext, text).post(link.kind, link.draftId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A drafts store that cannot be read right now is a reminder not posted, not a crash and not a retry.
        }
        return Result.success()
    }

    companion object {
        const val KIND = "kind"
        const val DRAFT = "draft"
    }
}
