package com.payslipmax.pdfparser.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Brings the reminder schedule back after a reboot. The system drops queued jobs when the phone restarts, and the app is not running to
 * rebuild them, so reminders would stay lost until the officer next opened it (found on the Pixel 9, 2026-10-26). When the system says boot is
 * complete this rebuilds the schedule once from the drafts ([ReminderSync.rebuildOnce]). Not exported (only the system sends the broadcast), it
 * checks the action anyway, does nothing while Part II is off (no sync is bound), and logs nothing.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    internal suspend fun handle(
        intent: Intent?,
        sync: ReminderSync?,
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            sync?.rebuildOnce()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A drafts store that cannot be read at boot is a schedule not rebuilt, never a crash; the next app launch rebuilds it.
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val sync = GlobalContext.getOrNull()?.getOrNull<ReminderSync>()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(intent, sync)
            } finally {
                pending?.finish()
            }
        }
    }
}
