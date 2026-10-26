package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.FakeReminderScheduler
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.setDevOverride
import com.payslipmax.pdfparser.ui.setPremiumEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the sync is joined to the entitlement in one place. With the feature off nothing may start (and Koin must not even be asked); with it
 * on, an officer who is not Premium has nothing scheduled and a Premium one has the schedule, so the join cannot be silently dropped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StartPart2RemindersTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scheduler = FakeReminderScheduler()
    private val repository = FakePart2Repository()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = PayslipViewModel(PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined), isFreeLaunchModeProvider = { false })
        viewModel.setDevOverride(DevOverride.FOLLOW_FLAG)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val koin
        get() =
            koinApplication {
                modules(module { single { ReminderSync(repository, scheduler, ReminderPlanner { 0L }, { 0L }, dispatcher) } })
            }.koin

    @Test
    fun withTheFeatureOffNothingIsStarted() {
        startPart2Reminders(koin, viewModel, enabled = false)

        assertEquals(emptyList(), scheduler.calls)
    }

    @Test
    fun anOfficerWhoIsNotPremiumHasNothingScheduled() {
        viewModel.setPremiumEnabled(false)

        startPart2Reminders(koin, viewModel, enabled = true)

        assertEquals(listOf(emptyList()), scheduler.calls, "the system is told to hold nothing")
    }

    @Test
    fun aPremiumOfficerIsFollowedAndALapseEmptiesTheSystemAgain() {
        viewModel.setPremiumEnabled(true)
        startPart2Reminders(koin, viewModel, enabled = true)
        assertEquals(1, scheduler.calls.size)

        viewModel.setPremiumEnabled(false)

        assertEquals(emptyList(), scheduler.pending)
    }

    @Test
    fun aKoinWithNoSyncDoesNotCrash() {
        startPart2Reminders(koinApplication { modules(module { }) }.koin, viewModel, enabled = true)
    }
}
