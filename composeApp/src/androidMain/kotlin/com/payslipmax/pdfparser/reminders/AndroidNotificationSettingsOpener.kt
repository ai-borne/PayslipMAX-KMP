package com.payslipmax.pdfparser.reminders

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import com.payslipmax.pdfparser.part2.reminders.NotificationSettingsOpener

/**
 * Opens this app's notification settings (`ACTION_APP_NOTIFICATION_SETTINGS`, Android 8 and later; the app's minimum is 8). The screen is the
 * system's: no component of ours is named, and nothing about a draft is sent. [start] starts the intent from the screen's Context; a Context
 * that is not an Activity ([fromActivity] false) cannot start it without a new task. A phone with no such screen is left as it is: the
 * button does nothing rather than crash.
 */
class AndroidNotificationSettingsOpener(
    private val packageName: String,
    private val fromActivity: Boolean,
    private val start: (Intent) -> Unit,
) : NotificationSettingsOpener {
    override fun open() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (!fromActivity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            start(intent)
        } catch (_: ActivityNotFoundException) {
            // Nothing handles it on this phone: the button has no other screen to go to.
        }
    }
}
