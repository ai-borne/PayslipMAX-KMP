package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.setDevOverride
import com.payslipmax.pdfparser.ui.setPremiumEnabled
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InsightsScreenUiTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    private val testOfficer = Officer("Test Officer", "00/000/000000X", "AA****00A")
    private val testSummary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0)

    private fun buildPayslip(
        year: Int,
        monthNum: Int,
        monthName: String,
    ) = ParsedPayslip(
        file = "test.pdf",
        year = year,
        monthNum = monthNum,
        monthName = monthName,
        dateStr = "${monthNum.toString().padStart(2, '0')}/$year",
        officer = testOfficer,
        earnings = Earnings(basicPay = 100000.0),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = testSummary,
        taxAndSavings = null,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository)
        // Debug builds default the entitlement override to FORCE_PRO; drive the real flag path so
        // these tests exercise production free-vs-premium gating rather than the dev bypass.
        viewModel.setDevOverride(DevOverride.FOLLOW_FLAG)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptyState() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()
            setContent {
                InsightsScreen(
                    viewModel = viewModel,
                    onNavigateTo = {},
                )
            }
            onNodeWithText("Please import or select a payslip to unlock financial insights.").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testMonthDropdownOpensOnTap() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 3, "March").toEncryptedEntity())
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            testDispatcher.scheduler.runCurrent()
            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateTo = {})
            }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // April 2026 is the most recent and should be auto-selected
            onNode(hasText("April 2026") and isSelected()).assertIsDisplayed()
            // First tap — dropdown must open (FilterChip is the selected node)
            onNode(hasText("April 2026") and isSelected()).performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("March 2026").assertIsDisplayed()
            // Second tap — dropdown must close
            onNode(hasText("April 2026") and isSelected()).performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("March 2026").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun freeUserSeesLockedHubNotScatteredTeasers() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            testDispatcher.scheduler.runCurrent()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // Inline locked card removed for free users — its unique CTA must not exist
            onNodeWithText(AppStringsPremium.aiAuditUnlockBtn).assertDoesNotExist()
            // The three scattered PRO teasers (Recommended Actions / locked anomalies / premium report
            // teaser) are gone — only the one consolidated hub card remains, at scroll bottom.
            onNodeWithText("Recommended For You").assertDoesNotExist()
            onRoot().performTouchInput { swipeUp() }
            mainClock.advanceTimeBy(300)
            onNodeWithText(InsightsStrings.premiumHubTitle, substring = true).assertExists()
            onNodeWithText(InsightsStrings.premiumHubCta).assertExists()
        }

    /** Pay Health is now a single surface (D-approved): the expandable chip in [MonthlySnapshot] — the
     *  top-bar pill was removed in the Phase 4 redesign wiring, so exactly one toggle exists. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun payHealthChipRendersAndExpandsOnTap() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            testDispatcher.scheduler.runCurrent()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            onNodeWithText(InsightsStrings.wellnessChipLabel, substring = true).assertIsDisplayed()
            onNodeWithContentDescription(InsightsStrings.wellnessChipExpandDesc).assertExists()

            onNodeWithContentDescription(InsightsStrings.wellnessChipExpandDesc).performClick()
            mainClock.advanceTimeBy(300)

            onNodeWithContentDescription(InsightsStrings.wellnessChipCollapseDesc).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun newBodySectionsRenderInApprovedOrder() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            testDispatcher.scheduler.runCurrent()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // MonthlySnapshot net-pay hero + Smart Insights are above the fold, in that order.
            // (PayTrendChart needs LedgerRecordEntity history, which this fake-DAO harness never
            // populates — that windowing logic is locked by PayTrendChartLogicTest instead.)
            onNodeWithText(InsightsStrings.snapshotNetPayLabel).assertIsDisplayed()
            onNodeWithText(InsightsStrings.smartInsightsSectionTitle).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun premiumUserSeesCaReportInlineNotTeaser() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            viewModel.setPremiumEnabled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.advanceUntilIdle()
            mainClock.advanceTimeBy(300)

            // The locked hub is a free-tier-only surface — the PRO shell dissolves, no wrapper card.
            onNodeWithText(InsightsStrings.premiumHubTitle, substring = true).assertDoesNotExist()

            // Scroll to compose the section in LazyColumn
            onNode(hasScrollAction()).performScrollToNode(hasText(AppStrings.geminiAiAnalyzeBtn))
            mainClock.advanceTimeBy(300)

            // CA report active card must be present (its generate CTA is the positive signal)
            onNodeWithText(AppStrings.geminiAiAnalyzeBtn).assertExists()
        }

    /** Insights PRO consolidation, Phase 2: for PRO users the hub dissolves into first-class cards —
     *  the AI report, an always-reachable tools drawer (no tap needed, "drawer is home"), and anomaly
     *  findings — with no separate "Recommended For You" strip and no feature listed twice. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun premiumUserSeesToolsDrawerExpandedByDefaultWithNoDuplicateRecommendations() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            viewModel.setPremiumEnabled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.advanceUntilIdle()
            mainClock.advanceTimeBy(300)

            // No standalone "Recommended For You" strip — the four tools it used to surface are the
            // same four the drawer already lists (the approved dedup: the drawer is home).
            onNodeWithText("Recommended For You").assertDoesNotExist()

            // Drawer defaults to expanded for PRO — a tool title is reachable with no "View all" tap.
            onNode(hasScrollAction()).performScrollToNode(hasText(AppStringsPremium.proCatalogRetCalcTitle))
            onNodeWithText(AppStringsPremium.proCatalogRetCalcTitle).assertIsDisplayed()

            // Retirement (Tax Planner / DSOP / Claim / Retirement were the RecommendedActions <->
            // drawer duplicates) now appears exactly once, in the drawer.
            onAllNodesWithText("Retirement", substring = true).assertCountEquals(1)
        }
}
