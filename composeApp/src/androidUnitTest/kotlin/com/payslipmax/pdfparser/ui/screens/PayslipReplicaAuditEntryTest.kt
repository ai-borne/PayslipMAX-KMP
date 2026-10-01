package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.selectHistoryDetailPayslip
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
import kotlin.test.assertEquals

/**
 * "Audit this month" must open Pay Audit on the payslip the user is *looking at*, not on whatever month
 * the dashboard happened to have selected. The month travels through the existing selected-payslip state
 * into [PayAuditViewModel.setInputs] (requestedMonth) — no new global.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayslipReplicaAuditEntryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    private fun slip(month: Int) =
        ParsedPayslip(
            file = "t.pdf", year = 2024, monthNum = month, monthName = "M", dateStr = "0$month/2024",
            officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
            earnings = Earnings(basicPay = 100000.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
            taxAndSavings = null,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dao = FakePayslipDao()
        val repository = PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined)
        runBlocking { listOf(7, 8).forEach { dao.insertPayslip(slip(it).toEncryptedEntity()) } }
        viewModel = PayslipViewModel(repository)
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
    fun auditThisMonthSelectsTheViewedPayslipAndOpensPayAudit() =
        runComposeUiTest {
            // The viewed payslip (Jul) is not the app-wide selection (the latest, Aug).
            viewModel.selectPayslip(slip(8))
            viewModel.selectHistoryDetailPayslip("07/2024")
            testDispatcher.scheduler.runCurrent()
            val opened = mutableListOf<Screen>()
            setContent { PayslipReplicaDetailScreen(viewModel = viewModel, onBack = {}, onNavigateTo = { opened += it }) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithTag("replica_audit_action_card").performScrollTo().performClick()
            testDispatcher.scheduler.runCurrent()

            assertEquals(listOf(Screen.PayAudit), opened)
            assertEquals("07/2024", viewModel.uiState.value.selectedPayslip?.dateStr)
        }

    @Test
    fun payAuditOpensOnTheMonthTheCtaSelected() {
        viewModel.selectPayslip(slip(7))
        testDispatcher.scheduler.runCurrent()
        val payAudit =
            PayAuditViewModel(
                engine = { _, _, _ -> EngineResult(healthScore = 0, anomalies = emptyList(), monthlySavingRate = 0.0, taxRatio = 0.0) },
                dispatcher = testDispatcher,
            )
        val state = viewModel.uiState.value
        val requested = state.selectedPayslip?.let { PayMonth(it.year, it.monthNum) }

        payAudit.setInputs(state.payslips, hasAccess = true, requestedMonth = requested)

        assertEquals(PayMonth(2024, 7), payAudit.uiState.value.selectedMonth)
    }
}
