package com.payslipmax.pdfparser.part2.reminders

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** WHY: the iOS answer must survive a new process, and the test must not leave the real key set on the simulator. */
class ReminderPromptStoreIosTest {
    private val key = "reminder_permission_asked_test"

    @BeforeTest
    @AfterTest
    fun clean() = NSUserDefaults.standardUserDefaults.removeObjectForKey(key)

    @Test
    fun aFreshDeviceHasNotBeenAskedAndTheAnswerSurvivesANewStore() {
        assertFalse(IosReminderPromptStore(key).wasAsked())

        IosReminderPromptStore(key).markAsked()

        assertTrue(IosReminderPromptStore(key).wasAsked())
    }

    @Test
    fun theShippedKeyIsTheOneTheAppUses() {
        assertTrue(IosReminderPromptStore.KEY == "reminder_permission_asked")
    }
}
