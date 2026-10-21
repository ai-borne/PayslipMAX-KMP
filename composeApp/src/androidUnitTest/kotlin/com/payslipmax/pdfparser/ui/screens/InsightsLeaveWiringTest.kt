package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The Insights tab as both platforms host it: the leave-cap card comes from the ViewModel's leave bank summary, so it follows
 * the latest statement, not the selected month (owner decision: every month shows it), and it follows the kill switch, so a
 * release built with the flag off never shows it, even over the cap.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class InsightsLeaveWiringTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dec2025 = payslip(2025, 12, table(OVER_CAP_DAYS), file = "dec.pdf")
    private val jul2025 = payslip(2025, 7, leave = null, file = "jul.pdf")
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dao = FakePayslipDao()
        runBlocking { listOf(dec2025, jul2025).forEach { dao.insertPayslip(it.toEncryptedEntity()) } }
        viewModel = PayslipViewModel(PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined))
        testDispatcher.scheduler.runCurrent()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun anOlderMonthWithoutATableShowsTheLeaveCapCardExactlyWhenTheKillSwitchIsOn() =
        runComposeUiTest {
            viewModel.selectPayslip(jul2025)
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // Jun 2025 isn't in history, so Jul 2025 takes the first-statement path; its card proves the section is composed.
            onNodeWithText(InsightsStrings.smartInsightsFirstStatementTitle).assertExists()
            val card = onNodeWithText(LeaveBankStrings.capInsightTitle)
            if (isLeaveBankEnabled()) card.assertExists() else card.assertDoesNotExist()
        }
}
