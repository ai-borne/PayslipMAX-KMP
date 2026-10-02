package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.App
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakeOnboardingStorage
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.WithTestKoin
import com.payslipmax.pdfparser.testing.testAppModule
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.AppStringsOnboarding
import com.payslipmax.pdfparser.ui.theme.PayAuditEntryStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
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
 * Screens must take their collaborators from Koin, not construct them. The proof is behavioural: each test
 * supplies a Koin module whose answer differs from what a self-built default would give, so a screen that
 * still builds its own `OnboardingManager` / `PayAuditViewModel` fails on a real assertion.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KoinInjectedDependenciesUiTest {
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
        runBlocking { listOf(7, 8).forEach { dao.insertPayslip(slip(it).toEncryptedEntity()) } }
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
    fun entryHostShowsTheIssueCountOfTheViewModelKoinProvides() =
        runComposeUiTest {
            // A clean payslip: only an injected engine can report an issue, so "1 ..." proves the host used it.
            val oneIssue =
                Anomaly(type = "TPTA_ENTITLEMENT", field = "transportAllowance", amount = 5508.0, month = "08/2024", description = "TPTA is short.", expected = 11268.0, actual = 5760.0)
            val oneMonthTimeline = ServiceTimeline(listOf(TimelineMonth(PayMonth(2024, 8), 100000.0, null, null, null, null, false)), emptyList(), emptyList())
            val engine = { _: ParsedPayslip, _: ParsedPayslip?, _: List<ParsedPayslip> ->
                EngineResult(healthScore = 0, anomalies = listOf(oneIssue), monthlySavingRate = 0.0, taxRatio = 0.0, timeline = oneMonthTimeline)
            }
            val payslips = viewModel.uiState.value.payslips

            setContent {
                WithTestKoin(testAppModule(payAuditEngine = engine)) {
                    PayAuditEntryHost(selected = payslips.last(), payslips = payslips, onOpen = {})
                }
            }
            waitForIdle()

            onNodeWithText("1 ${PayAuditStrings.findingsCountSingular}").assertExists()
        }

    @Test
    fun payAuditScreenShowsTheOrientationSheetWhenKoinsStorageHasNotSeenIt() =
        runComposeUiTest {
            setContent {
                WithTestKoin(testAppModule(storage = FakeOnboardingStorage(hasSeenPayAuditIntro = false))) {
                    PayAuditScreen(viewModel = viewModel, onBack = {}, onNavigateTo = {})
                }
            }
            testDispatcher.scheduler.runCurrent()
            waitForIdle()

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertExists()
        }

    @Test
    fun payAuditScreenSkipsTheOrientationSheetWhenKoinsStorageHasSeenIt() =
        runComposeUiTest {
            setContent {
                WithTestKoin(testAppModule(storage = FakeOnboardingStorage(hasSeenPayAuditIntro = true))) {
                    PayAuditScreen(viewModel = viewModel, onBack = {}, onNavigateTo = {})
                }
            }
            testDispatcher.scheduler.runCurrent()
            waitForIdle()

            onNodeWithText(PayAuditEntryStrings.orientationTitle).assertDoesNotExist()
        }

    @Test
    fun dashboardHidesTheCoachmarkWhenKoinsStorageHasSeenIt() =
        runComposeUiTest {
            setContent {
                WithTestKoin(testAppModule(storage = FakeOnboardingStorage(hasCompletedOnboarding = true, hasSeenUploadCoachmark = true))) {
                    DashboardScreen(viewModel = viewModel, onPickPdf = {})
                }
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithTag("upload_coachmark").assertDoesNotExist()
        }

    @Test
    fun appHidesTheOnboardingCarouselWhenKoinsStorageHasCompletedIt() =
        runComposeUiTest {
            setContent {
                WithTestKoin(testAppModule(storage = FakeOnboardingStorage(hasCompletedOnboarding = true, hasSeenUploadCoachmark = true))) {
                    App(viewModel = viewModel, onPickPdf = {}, onOpenPdf = { _, _ -> })
                }
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStringsOnboarding.onboardingSlide1Title).assertDoesNotExist()
        }

    @Test
    fun appShowsTheOnboardingCarouselWhenKoinsStorageHasNotCompletedIt() =
        runComposeUiTest {
            setContent {
                WithTestKoin(testAppModule(storage = FakeOnboardingStorage(hasCompletedOnboarding = false))) {
                    App(viewModel = viewModel, onPickPdf = {}, onOpenPdf = { _, _ -> })
                }
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStringsOnboarding.onboardingSlide1Title).assertExists()
        }
}
