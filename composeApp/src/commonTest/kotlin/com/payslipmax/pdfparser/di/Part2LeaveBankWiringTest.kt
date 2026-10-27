package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankFeed
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakeLeaveBankSource
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.ui.PayslipViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * WHY: the draft form's rows are the rows of the app's ONE Leave Bank summary, handed over in one place. The kill switch decides whether the
 * form gets a source at all, and with Part II off nothing is attached (Koin is not even asked). A second derivation of the summary, or a
 * source quietly detached from the view model, would fill the officer's order from figures the Leave Bank screens do not show.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Part2LeaveBankWiringTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val dao = FakePayslipDao()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    private fun viewModel() = PayslipViewModel(PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined))

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val statement = LeaveAccumulation(listOf(accrued("2023-12-31", 30, 282), accrued("2024-12-31", 19, 301)))

    @Test
    fun theKillSwitchDecidesWhetherTheFormGetsASourceAtAll() {
        val source = FakeLeaveBankSource()
        assertSame(source, leaveBankFor(source, enabled = true))
        assertNull(leaveBankFor(source, enabled = false))
    }

    @Test
    fun withPartTwoOffNothingIsAttachedAndKoinIsNotAsked() =
        runTest {
            val feed = LeaveBankFeed()
            attachPart2LeaveBank(koinApplication { modules(module { single { feed } }) }.koin, viewModel(), enabled = false)
            assertNull(withTimeoutOrNull(1) { feed.rows.firstOrNull() }, "nothing was attached, so the feed says nothing")
        }

    @Test
    fun theFeedCarriesTheRowsOfTheOneSummaryTheViewModelDerivesAndFollowsIt() =
        runTest {
            dao.insertPayslip(payslip(2025, 12, statement).toEncryptedEntity())
            val viewModel = viewModel()
            val feed = LeaveBankFeed()
            attachPart2LeaveBank(koinApplication { modules(module { single { feed } }) }.koin, viewModel, enabled = true)
            val rows = feed.rows.first { it.isNotEmpty() }
            backgroundScope.launch(dispatcher) { viewModel.leaveBankSummary.collect {} }
            assertEquals(viewModel.leaveBankSummary.value?.entries, rows, "the very rows of the one summary, never a second derivation")
            assertEquals(statement.entries, rows)
        }

    @Test
    fun aKoinWithNoFeedDoesNotCrash() {
        attachPart2LeaveBank(koinApplication { modules(module { }) }.koin, viewModel(), enabled = true)
    }
}
