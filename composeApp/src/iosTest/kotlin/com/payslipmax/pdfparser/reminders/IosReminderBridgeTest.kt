package com.payslipmax.pdfparser.reminders

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderPolicy
import com.payslipmax.pdfparser.part2.reminders.ReminderNotificationText
import com.payslipmax.pdfparser.testing.FakePart2Repository
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the Kotlin half of iOS reminders is a translator, and a wrong key, kind or instant would put a reminder on the wrong day or double it.
 * It must hand Swift exactly the planner's reminders and the injected words, answer permission questions once, and never invent a gateway.
 */
class IosReminderBridgeTest {
    private class RecordingGateway : IosNotificationGateway {
        val sent = mutableListOf<List<IosReminderRequest>>()
        var asks = 0
        var requests = 0
        var answerCanAsk: Boolean? = true
        var answerRequest: Boolean? = true
        var late: IosPermissionListener? = null
        var answerTwice = false

        override fun replaceAll(requests: List<IosReminderRequest>) {
            sent += requests
        }

        override fun canAsk(listener: IosPermissionListener) {
            asks++
            answerCanAsk?.let(listener::onAnswer) ?: run { late = listener }
        }

        override fun request(listener: IosPermissionListener) {
            requests++
            answerRequest?.let(listener::onAnswer) ?: run { late = listener }
            if (answerTwice) listener.onAnswer(false)
        }
    }

    private val text = ReminderNotificationText("TITLE", "BODY", "CHANNEL", "CHANNEL-DESC")
    private val back = Reminder(ReminderKind.RETURN, "d1", 1_800_000_000_000L, CalendarDate(2027, 1, 15))
    private val add = Reminder(ReminderKind.ADD_ORDER, "d2", 1_800_100_000_000L, CalendarDate(2027, 1, 16), sequence = 3)

    @Test
    fun everyReminderBecomesOneRequestWithItsKeyKindDraftInstantAndTheInjectedWords() {
        val gateway = RecordingGateway()

        IosReminderScheduler(text) { gateway }.replaceAll(listOf(back, add))

        val requests = gateway.sent.single()
        assertEquals(listOf(back.key, add.key), requests.map { it.key })
        assertEquals(listOf("RETURN", "ADD_ORDER"), requests.map { it.kind })
        assertEquals(listOf("d1", "d2"), requests.map { it.draftId })
        assertEquals(listOf(back.at, add.at), requests.map { it.fireAtMillis })
        assertEquals(listOf("TITLE", "TITLE"), requests.map { it.title })
        assertEquals(listOf("BODY", "BODY"), requests.map { it.body })
    }

    @Test
    fun anEmptyListIsStillSentSoTheSystemIsEmptied() {
        val gateway = RecordingGateway()

        IosReminderScheduler(text) { gateway }.replaceAll(emptyList())

        assertEquals(listOf(emptyList()), gateway.sent)
    }

    @Test
    fun withNoGatewayRegisteredNothingHappensAndNothingCrashes() {
        IosReminderScheduler(text) { null }.replaceAll(listOf(back))
    }

    @Test
    fun theSystemsPendingLimitIsAboveWhatThePlannerEverGives() {
        assertTrue(ReminderPolicy().upcomingMax <= IosReminderScheduler.OS_PENDING_LIMIT, "iOS silently drops pending requests past 64")
        assertEquals(64, IosReminderScheduler.OS_PENDING_LIMIT)
    }

    @Test
    fun theRequestsTextFormHidesTheDraft() {
        assertFalse("d1" in IosReminderRequest("k", "RETURN", "d1", 1L, "t", "b").toString())
    }

    @Test
    fun permissionQuestionsAreAnsweredByTheGatewayAndAskedOnce() =
        runTest {
            val gateway = RecordingGateway()
            val permission = IosNotificationPermission { gateway }

            assertTrue(permission.canAsk())
            assertTrue(permission.request())
            gateway.answerCanAsk = false
            gateway.answerRequest = false
            assertFalse(permission.canAsk())
            assertFalse(permission.request())
            assertEquals(2, gateway.asks)
            assertEquals(2, gateway.requests)
        }

    @Test
    fun withNoGatewayThereIsNothingToAskAndNothingIsAllowed() =
        runTest {
            val permission = IosNotificationPermission { null }

            assertFalse(permission.canAsk())
            assertFalse(permission.request())
        }

    @Test
    fun aSecondAnswerFromSwiftIsIgnoredAndTheFirstWins() =
        runTest {
            val gateway = RecordingGateway().apply { answerTwice = true }

            assertTrue(IosNotificationPermission { gateway }.request(), "true came first; the later false must not crash or replace it")
        }

    @Test
    fun aTapIsHandedToThePendingLinkOnlyWhenPartTwoIsBound() =
        runTest {
            val repository = FakePart2Repository()
            val pending = PendingPart2Link(repository)
            val on = koinApplication { modules(module { single { pending } }) }.koin

            offerReminderTap(on, "RETURN", "d1")
            assertEquals("d1", pending.link.value?.draftId)

            val offPending = PendingPart2Link(repository)
            offerReminderTap(koinApplication { modules(module { }) }.koin, "RETURN", "d1")
            offerReminderTap(null, "RETURN", "d1")
            assertNull(offPending.link.value)
        }
}
