package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.crypto.ContextHolder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** WHY: "asked once, never again" is only true if the answer survives the process and a store that cannot save never lets the prompt show. */
@RunWith(RobolectricTestRunner::class)
class ReminderPromptStoreAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun aFreshDeviceHasNotBeenAskedAndTheAnswerSurvivesANewStore() {
        ContextHolder.context = RuntimeEnvironment.getApplication()
        assertFalse(AndroidReminderPromptStore().wasAsked())

        AndroidReminderPromptStore().markAsked()

        assertTrue(AndroidReminderPromptStore().wasAsked(), "a new instance (a new process) still knows")
    }

    @Test
    fun theFlagLivesInPrivatePreferencesAndNotInTheOnboardingFile() {
        ContextHolder.context = RuntimeEnvironment.getApplication()

        AndroidReminderPromptStore().markAsked()

        val onboarding = RuntimeEnvironment.getApplication().getSharedPreferences("payslipmax_onboarding_prefs", 0)
        assertFalse(onboarding.contains(AndroidReminderPromptStore.KEY))
        assertTrue(RuntimeEnvironment.getApplication().getSharedPreferences(AndroidReminderPromptStore.PREFS, 0).getBoolean(AndroidReminderPromptStore.KEY, false))
    }

    @Test
    fun withNoContextTheOfficerIsTreatedAsAlreadyAsked() {
        ContextHolder.context = null

        assertTrue(AndroidReminderPromptStore().wasAsked(), "an unknown state must not show a prompt")
    }
}
