package com.payslipmax.pdfparser.ui.screens.guide.part2

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.testing.FakeReminderPromptStore
import com.payslipmax.pdfparser.ui.screens.guide.part2.reminders.ReminderPromptModel
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY (DECISIONS Round 44, plan item 39): after a decline the Due screen must say reminders can be turned on in the system settings, with a
 * button that opens this app's notification settings, and say nothing while reminders work or before the officer was asked. Run on the real
 * Android permission wrapper over Robolectric's notification state, so the whole path (grant, switch, resume, button) is exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2DueSettingsNoteUiTest : Part2DueUiBase() {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private fun phone(
        granted: Boolean,
        switchedOn: Boolean = true,
    ) {
        if (granted) shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS) else shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).setNotificationsEnabled(switchedOn)
    }

    private fun settled(asked: Boolean) {
        prompt = ReminderPromptModel(MutableStateFlow(false), FakeReminderPromptStore(asked = asked))
    }

    private fun noteShown() = count(Part2ReminderStrings.settingsNote).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun afterADeclineTheDueScreenSaysRemindersCanBeTurnedOnInTheSettingsAndOffersTheButton() {
        phone(granted = false)
        hold("d1")
        show(atDue())

        see(Part2ReminderStrings.settingsNote).assertExists()
        see(Part2ReminderStrings.settingsOpen).assertExists()
    }

    @Test
    fun nothingIsSaidWhileRemindersWork() {
        phone(granted = true)
        hold("d1")
        show(atDue())

        assertEquals(false, noteShown())
        assertEquals(0, count(Part2ReminderStrings.settingsOpen).fetchSemanticsNodes().size)
    }

    @Test
    fun nothingIsSaidBeforeTheOfficerWasAsked() {
        phone(granted = false)
        settled(asked = false)
        hold("d1")
        show(atDue())

        assertEquals(false, noteShown())
    }

    @Test
    fun theNoteAlsoShowsWhenTheOfficerSwitchedNotificationsOffAfterGrantingThem() {
        phone(granted = true, switchedOn = false)
        hold("d1")
        show(atDue())

        see(Part2ReminderStrings.settingsNote).assertExists()
    }

    @Test
    fun theButtonOpensThisAppsNotificationSettingsAndNothingElseHappens() {
        phone(granted = false)
        hold("d1")
        show(atDue())

        tap(Part2ReminderStrings.settingsOpen)

        val intent: Intent = assertNotNull(shadowOf(app).nextStartedActivity)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(app.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertNull(shadowOf(app).nextStartedActivity, "one tap, one screen")
        see(Part2ReminderStrings.settingsNote).assertExists()
    }

    @Test
    fun theNoteGoesWhenTheOfficerTurnsRemindersOnAndComesBackToTheApp() {
        phone(granted = false)
        hold("d1")
        show(atDue())
        see(Part2ReminderStrings.settingsNote).assertExists()

        phone(granted = true)
        composeRule.runOnUiThread {
            val lifecycle = composeRule.activity.lifecycle as LifecycleRegistry
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        settle()

        assertEquals(false, noteShown())
    }

    @Test
    fun aLapsedOfficerSeesTheUpgradePanelAndNoNote() {
        phone(granted = false)
        hold("d1")
        show(atDue(), part2Unlocked = false)

        assertEquals(false, noteShown())
    }
}
