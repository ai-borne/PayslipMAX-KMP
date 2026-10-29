package com.payslipmax.pdfparser.reminders

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the button must open OUR app's notification settings (not another app's, not the whole list), through a system action only, and must not
 * crash a phone that cannot show it. From something that is not an Activity the system needs a new task to start it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidNotificationSettingsOpenerTest {
    private val started = mutableListOf<Intent>()

    private fun opener(
        fromActivity: Boolean,
        start: (Intent) -> Unit = { started += it },
    ) = AndroidNotificationSettingsOpener("in.example.app", fromActivity, start)

    @Test
    fun openingStartsTheNotificationSettingsOfThisAppOnce() {
        opener(fromActivity = true).open()

        val intent = started.single()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals("in.example.app", intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertNull(intent.component, "a system action, not one of our own screens")
        assertNull(intent.data)
    }

    @Test
    fun aNewTaskIsRequestedOnlyWhenNotStartedFromAnActivity() {
        opener(fromActivity = true).open()
        opener(fromActivity = false).open()

        assertFalse(started[0].flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(started[1].flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun aPhoneThatCannotShowTheScreenDoesNotCrashTheApp() {
        opener(fromActivity = true) { throw ActivityNotFoundException() }.open()
    }
}
