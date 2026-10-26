package com.payslipmax.pdfparser.reminders

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationManager

/**
 * The fixtures of the reminder delivery tests: a fake repository holding one printed leave (synthetic dates only), a clock on the day the
 * return reminder falls due, and the process-wide Koin the worker reads (stopped afterwards, because Koin's context is global).
 */
abstract class RemindersAndroidTestBase {
    protected val context: Application = ApplicationProvider.getApplicationContext()
    protected val repository = FakePart2Repository()
    protected val pending = PendingPart2Link(repository)
    protected val india = 19_800_000L
    protected val returnDay = PreClaimDates.toUtcMillis(CalendarDate(2026, 10, 10)) + 9 * 3_600_000L - india
    protected var clock = returnDay + 60_000L
    protected val planner = ReminderPlanner { india }

    @Before
    fun startKoinWithTheReminderBindings() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { ReminderFireCheck(repository, planner) { clock } }
                    single { Part2ReminderStrings.notificationText() }
                    single { pending }
                },
            )
        }
    }

    @After
    fun stopKoinAgain() = stopKoin()

    /** A printed annual leave from 5 to 9 October, so the return reminder falls on 10 October at 09:00. */
    protected fun printedLeave(
        id: String = "d1",
        dismissed: Set<String> = emptySet(),
    ): Part2Draft {
        val request = OccurrenceRequest("GTDAL", mapOf("leave_from" to SlotValue.DateValue(CalendarDate(2026, 10, 5)), "leave_to" to SlotValue.DateValue(CalendarDate(2026, 10, 9))))
        val printedAt = returnDay - 9L * 86_400_000L
        return Part2Draft(id, LeaveEventInput(listOf(request)), emptyMap(), 1, 1, PrintedSnapshot(printedAt, emptyMap(), emptyList()), dismissed = dismissed)
    }

    protected fun save(draft: Part2Draft) = runBlocking { repository.saveDraft(draft) }

    private val manager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    protected val shadowManager: ShadowNotificationManager get() = shadowOf(manager)

    protected fun posted(): List<Notification> = shadowManager.allNotifications
}
