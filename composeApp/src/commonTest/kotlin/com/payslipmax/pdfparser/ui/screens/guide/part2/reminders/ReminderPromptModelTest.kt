package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import com.payslipmax.pdfparser.testing.FakeNotificationPermission
import com.payslipmax.pdfparser.testing.FakeReminderPromptStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (DECISIONS Rounds 10 and 28): the officer is asked once, the first time a draft sets a reminder, with our own sentence before the
 * system dialog, and a decline is final. A second ask would nag; an ask before there is a reminder would be a request with no reason.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderPromptModelTest {
    private val store = FakeReminderPromptStore()
    private val permission = FakeNotificationPermission()
    private val has = MutableStateFlow(true)

    private fun model() = ReminderPromptModel(has, store)

    @Test
    fun withNoReminderNothingIsShownAndNothingIsRecorded() =
        runTest {
            has.value = false
            val model = model()

            model.onRemindersExist(permission)

            assertFalse(model.visible.value)
            assertFalse(store.wasAsked())
            assertEquals(0, permission.requests)
        }

    @Test
    fun theFirstReminderOnADeviceThatCanStillBeAskedShowsTheExplainer() =
        runTest {
            val model = model()

            model.onRemindersExist(permission)

            assertTrue(model.visible.value)
            assertEquals(0, permission.requests, "the system dialog waits for the officer's Allow")
            assertFalse(store.wasAsked(), "asking is not asked yet")
        }

    @Test
    fun anOfficerAlreadyAskedIsNeverShownTheExplainerAgain() =
        runTest {
            store.markAsked()
            val model = model()

            model.onRemindersExist(permission)

            assertFalse(model.visible.value)
            assertEquals(0, permission.requests)
        }

    @Test
    fun aDeviceThatHasNothingToAskIsMarkedAskedWithoutAnyPrompt() =
        runTest {
            permission.askable = false
            val model = model()

            model.onRemindersExist(permission)

            assertFalse(model.visible.value)
            assertTrue(store.wasAsked(), "no dialog exists here (older Android, or already decided), so it is settled")
        }

    @Test
    fun allowShowsTheSystemDialogOnceAndRecordsTheAnswerWhateverItIs() =
        runTest {
            for (grants in listOf(true, false)) {
                val store = FakeReminderPromptStore()
                val permission = FakeNotificationPermission(grants = grants)
                val model = ReminderPromptModel(has, store)
                model.onRemindersExist(permission)

                model.allow(permission)

                assertEquals(1, permission.requests)
                assertFalse(model.visible.value)
                assertTrue(store.wasAsked(), "granted=$grants")
            }
        }

    @Test
    fun theAnswerIsRecordedBeforeTheSystemDialogSoAKillMidwayNeverAsksTwice() =
        runTest(UnconfinedTestDispatcher()) {
            val gate = CompletableDeferred<Boolean>()
            val pending =
                object : NotificationPermission {
                    override suspend fun canAsk() = true

                    override suspend fun request(): Boolean = gate.await()
                }
            val model = model()
            model.onRemindersExist(pending)

            val job = launch { model.allow(pending) }

            assertTrue(store.wasAsked(), "the dialog is still open")
            assertFalse(model.visible.value)
            gate.complete(false)
            job.join()
        }

    @Test
    fun notNowIsADeclineThatShowsNoSystemDialogAndIsNeverAskedAgain() =
        runTest {
            val model = model()
            model.onRemindersExist(permission)

            model.notNow()

            assertFalse(model.visible.value)
            assertTrue(store.wasAsked())
            assertEquals(0, permission.requests)
            model.onRemindersExist(permission)
            assertFalse(model.visible.value, "a later reminder does not bring it back")
        }

    @Test
    fun checkingAgainWhileTheExplainerIsUpChangesNothing() =
        runTest {
            val model = model()
            model.onRemindersExist(permission)

            model.onRemindersExist(permission)

            assertTrue(model.visible.value)
            assertFalse(store.wasAsked())
        }
}
