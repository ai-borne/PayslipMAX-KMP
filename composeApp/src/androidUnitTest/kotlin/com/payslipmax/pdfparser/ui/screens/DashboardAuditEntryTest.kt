package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.onboarding.AndroidOnboardingStorage
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.PayslipViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The dashboard shows the Pay Audit banner once there are payslips, and tapping it navigates. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardAuditEntryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ContextHolder.context = RuntimeEnvironment.getApplication()
        AndroidOnboardingStorage().saveHasSeenUploadCoachmark(true)
        val dao = FakePayslipDao()
        val repository = PayslipRepository(dao, FakePdfParser(), testDispatcher)
        val slip =
            ParsedPayslip(
                file = "t.pdf", year = 2024, monthNum = 8, monthName = "August", dateStr = "08/2024",
                officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
                earnings = Earnings(basicPay = 100000.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
                taxAndSavings = null,
            )
        runBlocking { dao.insertPayslip(slip.toEncryptedEntity()) }
        viewModel = PayslipViewModel(repository)
        testDispatcher.scheduler.runCurrent()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        ContextHolder.context = null
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingTheDashboardBannerNavigatesToPayAudit() =
        runComposeUiTest {
            var opened = 0
            setContent { DashboardScreen(viewModel = viewModel, onPickPdf = {}, onNavigateToAudit = { opened++ }) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithTag("dashboard_audit_banner_card").performScrollTo().performClick()

            assertEquals(1, opened)
        }
}
