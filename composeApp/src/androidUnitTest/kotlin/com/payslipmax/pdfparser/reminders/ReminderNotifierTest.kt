package com.payslipmax.pdfparser.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import com.payslipmax.pdfparser.MainActivity
import com.payslipmax.pdfparser.R
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (plan, Security): the notification is the one thing Part II puts outside the app. It must carry the same generic sentence for every
 * reminder, nothing of the draft, and a tap target that no other app can redirect or rewrite: an explicit, immutable PendingIntent whose
 * extras are the reminder kind and the draft id and nothing else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderNotifierTest : RemindersAndroidTestBase() {
    private val notifier get() = ReminderNotifier(context, Part2ReminderStrings.notificationText())

    @Test
    fun aReminderIsPostedWithTheGenericSentenceAndTheAppsOwnIcon() {
        assertTrue(notifier.post(ReminderKind.RETURN, "d1"))

        val note = posted().single()
        assertEquals(Part2ReminderStrings.notificationTitle, note.extras.getString("android.title"))
        assertEquals(Part2ReminderStrings.notificationBody, note.extras.getString("android.text"))
        assertEquals(R.drawable.ic_stat_reminder, note.smallIcon.resId)
        assertTrue(note.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0, "a tap removes it")
        assertEquals(android.app.Notification.CATEGORY_REMINDER, note.category)
    }

    @Test
    fun nothingOfTheDraftIsInTheNotificationItself() {
        notifier.post(ReminderKind.YEAR_END, "draft-secret-0001")

        val note = posted().single()
        val everything = note.extras.keySet().mapNotNull { note.extras.get(it)?.toString() }.joinToString(" ")
        assertFalse("draft-secret-0001" in everything, "the id travels in the tap intent only")
        assertFalse("YEAR_END" in everything)
    }

    @Test
    fun theTapTargetIsExplicitImmutableAndCarriesOnlyTheKindAndTheDraftId() {
        notifier.post(ReminderKind.ADD_ORDER, "d1")

        val tap = shadowOf(posted().single().contentIntent)
        assertTrue(tap.isActivityIntent)
        assertTrue(tap.flags and PendingIntent.FLAG_IMMUTABLE != 0, "no other app can fill in or rewrite it")
        val intent = tap.savedIntent
        assertEquals(ComponentName(context, MainActivity::class.java), intent.component, "explicit: it names our own activity")
        assertNull(intent.action)
        assertNull(intent.data)
        assertEquals(setOf(ReminderLinkIntent.EXTRA_KIND, ReminderLinkIntent.EXTRA_DRAFT), intent.extras!!.keySet())
        assertEquals("ADD_ORDER", intent.getStringExtra(ReminderLinkIntent.EXTRA_KIND))
        assertEquals("d1", intent.getStringExtra(ReminderLinkIntent.EXTRA_DRAFT))
        for (flag in listOf(Intent.FLAG_ACTIVITY_SINGLE_TOP, Intent.FLAG_ACTIVITY_CLEAR_TOP, Intent.FLAG_ACTIVITY_NEW_TASK)) {
            assertTrue(intent.flags and flag != 0, "an open app is reused, not stacked (flag $flag)")
        }
    }

    @Test
    fun twoRemindersGetTwoTapTargetsThatDoNotOverwriteEachOther() {
        notifier.post(ReminderKind.RETURN, "d1")
        notifier.post(ReminderKind.RETURN, "d2")

        val drafts = posted().map { shadowOf(it.contentIntent).savedIntent.getStringExtra(ReminderLinkIntent.EXTRA_DRAFT) }
        assertEquals(setOf("d1", "d2"), drafts.toSet())
    }

    @Test
    fun twoKindsOfReminderForOneDraftAreTwoNotifications() {
        notifier.post(ReminderKind.RETURN, "d1")
        notifier.post(ReminderKind.YEAR_END, "d1")

        assertEquals(setOf("RETURN", "YEAR_END"), posted().map { shadowOf(it.contentIntent).savedIntent.getStringExtra(ReminderLinkIntent.EXTRA_KIND) }.toSet())
    }

    @Test
    fun theSameReminderTwiceIsOneNotification() {
        notifier.post(ReminderKind.ADD_ORDER, "d1")
        notifier.post(ReminderKind.ADD_ORDER, "d1")

        assertEquals(1, posted().size)
    }

    @Test
    fun theChannelCarriesOurWordsAndIsCreatedOnce() {
        notifier.post(ReminderKind.RETURN, "d1")
        notifier.post(ReminderKind.RETURN, "d2")

        val channel = shadowManager.notificationChannels.single() as android.app.NotificationChannel
        assertEquals(Part2ReminderStrings.channelName, channel.name.toString())
        assertEquals(Part2ReminderStrings.channelDescription, channel.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun withNotificationsTurnedOffNothingIsPostedAndTheCallerIsTold() {
        shadowManager.setNotificationsEnabled(false)

        assertFalse(notifier.post(ReminderKind.RETURN, "d1"))

        assertEquals(emptyList(), posted())
    }
}
