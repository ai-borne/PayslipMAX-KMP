package com.payslipmax.pdfparser.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.payslipmax.pdfparser.R
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText

/**
 * Posts the Part II reminder notification: one generic sentence from [ReminderNotificationText] (injected, so the platform owns no copy),
 * the app's small icon, and a tap target that is an explicit, immutable PendingIntent (see [ReminderLinkIntent]). The notification carries
 * nothing else: no draft, code, order number or date, and nothing is logged. A reminder of the same kind for the same draft replaces its
 * earlier notification instead of piling up.
 */
class ReminderNotifier(
    private val context: Context,
    private val text: ReminderNotificationText,
) {
    /** Posts the reminder; false (and nothing posted) when the officer has notifications turned off. */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() below is the permission check, and a missing permission posts nothing.
    fun post(
        kind: ReminderKind,
        draftId: String,
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(text.channelName)
                .setDescription(text.channelDescription)
                .build(),
        )
        // The id is also the PendingIntent's request code: PendingIntents are matched without their extras, so each reminder needs its own.
        val id = "$kind:$draftId".hashCode()
        val tap = PendingIntent.getActivity(context, id, ReminderLinkIntent.tapIntent(context, kind, draftId), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setContentTitle(text.title)
                .setContentText(text.body)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
        manager.notify(id, note)
        return true
    }

    private companion object {
        const val CHANNEL_ID = "part2_reminders"
    }
}
