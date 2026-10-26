package com.payslipmax.pdfparser.reminders

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.reminders.ReminderScheduler
import java.util.concurrent.TimeUnit

/**
 * Hands the planner's reminders to WorkManager: one deferrable job per [Reminder.key] (the unique work name), which fires at about the
 * planner's instant (WorkManager is inexact: Doze or battery saver can delay a job, and DECISIONS Round 28 chose that over exact alarms and
 * their extra permission). A job survives a reboot. The delay is the planner's instant minus now and nothing more; this class works out no
 * date. The job holds the kind and the draft id only. WorkManager is fetched on first use, so building the scheduler needs no Android. [replaceAll] blocks on WorkManager's own queries, so call it off the main thread.
 */
class AndroidReminderScheduler(
    private val workManager: () -> WorkManager,
    private val now: () -> Long = System::currentTimeMillis,
) : ReminderScheduler {
    private val work get() = workManager()

    internal fun request(reminder: Reminder): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay((reminder.at - now()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KIND to reminder.kind.name, ReminderWorker.DRAFT to reminder.draftId))
            .addTag(TAG)
            .build()

    override fun replaceAll(reminders: List<Reminder>) {
        // Only jobs still waiting are cancelled: one that is running has fired and is posting its notification right now. Every listed
        // reminder is then queued afresh under its key (KEEP, so a job that is somehow running under that key is never cancelled).
        for (info in work.getWorkInfosByTag(TAG).get()) if (info.state == WorkInfo.State.ENQUEUED) work.cancelWorkById(info.id)
        for (reminder in reminders) work.enqueueUniqueWork(reminder.key, ExistingWorkPolicy.KEEP, request(reminder))
    }

    companion object {
        const val TAG = "part2_reminder"
    }
}
