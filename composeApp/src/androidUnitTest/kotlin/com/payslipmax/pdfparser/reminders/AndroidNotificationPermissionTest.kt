package com.payslipmax.pdfparser.reminders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: below Android 13 there is no notification permission to ask for, and from 13 the system dialog may be shown only when the permission
 * is missing. The wrapper must never launch a dialog that cannot appear, and must wait for the real answer when one can.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidNotificationPermissionTest {
    private var launched = 0
    private var granted = false
    private var enabled = true

    private fun permission(sdk: Int) = AndroidNotificationPermission(sdk, { granted }, { enabled }, { launched++ })

    @Test
    fun beforeAndroid13ThereIsNothingToAskAndNoDialogIsLaunched() =
        runTest {
            val permission = permission(32)

            assertFalse(permission.canAsk())
            assertTrue(permission.request(), "notifications are on by default there")
            enabled = false
            assertFalse(permission.request(), "unless the officer turned them off in settings")
            assertEquals(0, launched)
        }

    @Test
    fun onAndroid13WithTheGrantMissingTheDialogCanBeAsked() =
        runTest {
            assertTrue(permission(33).canAsk())
        }

    @Test
    fun onAndroid13WithTheGrantAlreadyGivenThereIsNothingToAsk() =
        runTest {
            granted = true
            val permission = permission(34)

            assertFalse(permission.canAsk())
            assertTrue(permission.request())
            assertEquals(0, launched)
        }

    @Test
    fun requestingLaunchesTheDialogOnceAndAnswersWithTheOfficersChoice() =
        runTest(UnconfinedTestDispatcher()) {
            for (choice in listOf(true, false)) {
                launched = 0
                val permission = permission(34)
                var answer: Boolean? = null
                val job = launch { answer = permission.request() }

                assertEquals(1, launched)
                assertEquals(null, answer, "it waits for the system dialog")
                permission.onResult(choice)
                job.join()

                assertEquals(choice, answer)
            }
        }

    @Test
    fun anAnswerWithNoQuestionOutstandingIsIgnored() =
        runTest {
            permission(34).onResult(true)
        }
}
