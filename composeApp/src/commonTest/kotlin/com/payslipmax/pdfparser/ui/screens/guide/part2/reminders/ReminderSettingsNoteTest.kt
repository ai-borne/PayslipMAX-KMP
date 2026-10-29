package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

import com.payslipmax.pdfparser.testing.FakeNotificationPermission
import com.payslipmax.pdfparser.testing.FakeReminderPromptStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (DECISIONS Round 44, plan item 39): an officer who declined has no way back unless the Due screen says reminders can be turned on in the
 * system settings. The note must appear only when that is true and useful (they were asked, and notifications are off), and must never become
 * a second ask: the decline stays final (Round 28). The decision is common code, so this runs for Android and iOS alike.
 */
class ReminderSettingsNoteTest {
    private val store = FakeReminderPromptStore()
    private val permission = FakeNotificationPermission(askable = true, grants = true, allowed = false)
    private val has = MutableStateFlow(true)

    private fun model() = ReminderPromptModel(has, store)

    @Test
    fun theNoteIsShownOnlyWhenAskedAndNotAllowed() {
        assertTrue(ReminderSettingsNote.isShown(asked = true, allowed = false))
        assertFalse(ReminderSettingsNote.isShown(asked = true, allowed = true), "reminders are on: nothing to say")
        assertFalse(ReminderSettingsNote.isShown(asked = false, allowed = false), "the explainer is still to come")
        assertFalse(ReminderSettingsNote.isShown(asked = false, allowed = true))
    }

    @Test
    fun beforeTheFirstAskTheExplainerIsShownAndTheNoteIsNot() =
        runTest {
            val model = model()

            model.onRemindersExist(permission)

            assertTrue(model.visible.value)
            assertFalse(model.settingsNoteShown(permission), "two prompts at once would be noise")
        }

    @Test
    fun afterNotNowTheNoteIsShownAndTheExplainerNeverReturns() =
        runTest {
            val model = model()
            model.onRemindersExist(permission)
            model.notNow()

            assertTrue(model.settingsNoteShown(permission))
            model.onRemindersExist(permission)
            assertFalse(model.visible.value, "the decline is final")
            assertEquals(0, permission.requests, "and the system dialog is not shown again")
        }

    @Test
    fun afterAllowThatTheSystemGrantedTheNoteIsNotShown() =
        runTest {
            val model = model()
            model.onRemindersExist(permission)
            model.allow(permission)

            assertFalse(model.settingsNoteShown(permission))
        }

    @Test
    fun afterAllowThatTheSystemRefusedTheNoteIsShown() =
        runTest {
            val refusing = FakeNotificationPermission(askable = true, grants = false)
            val model = model()
            model.onRemindersExist(refusing)
            model.allow(refusing)

            assertTrue(model.settingsNoteShown(refusing))
        }

    @Test
    fun theNoteGoesWhenTheOfficerLaterTurnsRemindersOnInTheSettings() =
        runTest {
            val model = model()
            model.onRemindersExist(permission)
            model.notNow()
            assertTrue(model.settingsNoteShown(permission))

            permission.allowed = true

            assertFalse(model.settingsNoteShown(permission))
        }

    @Test
    fun aDeviceWithNothingToAskAndNotificationsOffShowsTheNote() =
        runTest {
            val none = FakeNotificationPermission(askable = false, allowed = false)
            val model = model()

            model.onRemindersExist(none)

            assertFalse(model.visible.value)
            assertTrue(model.settingsNoteShown(none), "the device was settled silently, yet nothing can arrive")
        }

    @Test
    fun aDeviceWithNothingToAskAndNotificationsOnShowsNothing() =
        runTest {
            val none = FakeNotificationPermission(askable = false, allowed = true)
            val model = model()

            model.onRemindersExist(none)

            assertFalse(model.settingsNoteShown(none))
        }

    @Test
    fun readingTheNoteAsksNothingAndRecordsNothing() =
        runTest {
            val model = model()

            repeat(3) { model.settingsNoteShown(permission) }

            assertFalse(store.wasAsked())
            assertFalse(model.visible.value)
            assertEquals(0, permission.requests)
        }
}
