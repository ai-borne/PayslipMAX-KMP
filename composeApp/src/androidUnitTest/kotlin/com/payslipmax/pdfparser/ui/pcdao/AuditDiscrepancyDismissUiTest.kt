package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pcdao.engine.AllowanceCollisionCodes
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
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
import com.payslipmax.pdfparser.ui.screens.formatCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
class AuditDiscrepancyDismissUiTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ContextHolder.context = RuntimeEnvironment.getApplication()
        AndroidOnboardingStorage().saveHasSeenPcdaoAuditIntro(true)
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

    @Test
    fun alarmCard_showsDismissButton_andClickInvokesCallback() =
        runComposeUiTest {
            var dismissed = false
            val alarm = createAlarmDiscrepancy()

            setContent {
                AuditDiscrepancyCard(
                    discrepancy = alarm,
                    onDismiss = { dismissed = true },
                )
            }

            val dismissButton = onNodeWithTag(TestTags.DISMISS_ALARM_BUTTON)
            dismissButton.assertIsDisplayed()
            onNodeWithContentDescription(AppStringsPcdao.dismissAlarmDesc).assertIsDisplayed()

            dismissButton.performClick()
            assertEquals(true, dismissed)
        }

    @Test
    fun nonCriticalWarningCard_showsDismissButton() =
        runComposeUiTest {
            var dismissed = false
            val warning =
                AuditDiscrepancy(
                    id = "DISC_WARNING_1",
                    title = "Non-Critical Entitlement Underpayment",
                    type = DiscrepancyType.UNDERPAYMENT,
                    severity = DiscrepancySeverity.WARNING,
                    drawnAmount = 0.0,
                    entitledAmount = 5400.0,
                    netDue = 5400.0,
                    authority = "MoD Rule",
                    explanation = "Underpaid",
                    recommendedAction = "Claim dues",
                )

            setContent {
                AuditDiscrepancyCard(
                    discrepancy = warning,
                    onDismiss = { dismissed = true },
                )
            }

            val dismissButton = onNodeWithTag(TestTags.DISMISS_ALARM_BUTTON)
            dismissButton.assertIsDisplayed()
            dismissButton.performClick()
            assertEquals(true, dismissed)
        }

    @Test
    fun criticalRecoveryHazard_doesNotShowDismissButton_evenIfOnDismissProvided() =
        runComposeUiTest {
            val criticalHazard =
                AuditDiscrepancy(
                    id = "HAZARD_FIELD_RATION_COLLISION",
                    title = "Free Field Ration Concurrently Drawn with Cash RMA",
                    type = DiscrepancyType.RECOVERY_HAZARD,
                    severity = DiscrepancySeverity.CRITICAL,
                    drawnAmount = 24000.0,
                    entitledAmount = 0.0,
                    netDue = -24000.0,
                    authority = "Rule 174(B) DSR",
                    explanation = "Overdrawn cash ration",
                    recommendedAction = "Cease cash RMA",
                )

            setContent {
                AuditDiscrepancyCard(
                    discrepancy = criticalHazard,
                    onDismiss = { error("Should not be dismissable") },
                )
            }

            onNodeWithTag(TestTags.DISMISS_ALARM_BUTTON).assertDoesNotExist()
        }

    @Test
    fun alarmCard_withoutDismissCallback_doesNotShowDismissButton() =
        runComposeUiTest {
            val alarm = createAlarmDiscrepancy()

            setContent {
                AuditDiscrepancyCard(
                    discrepancy = alarm,
                    onDismiss = null,
                )
            }

            onNodeWithTag(TestTags.DISMISS_ALARM_BUTTON).assertDoesNotExist()
        }

    @Test
    fun mathDiffTable_withZeroNetDue_rendersCompliantNeutralAmount() =
        runComposeUiTest {
            val alarm = createAlarmDiscrepancy()

            setContent {
                AuditDiscrepancyCard(discrepancy = alarm)
            }

            onNodeWithText(AppStringsPcdao.colNetDue).assertIsDisplayed()
            onNodeWithText(formatCurrency(0.0)).assertIsDisplayed()
        }

    @Test
    fun pcdaoAuditScreen_wiringDismissDiscrepancy_dismissesAlarmInRealTime() =
        runComposeUiTest {
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
            val mockAssetProvider = MockUiAssetProvider()
            val rulesRepo = PcdaoRulesRepository(mockAssetProvider)
            val payslip = createMockPayslip("03/2026", 2026, 3, basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            val viewModel =
                PcdaoAuditViewModel(
                    payslipRepository = repository,
                    rulesRepository = rulesRepo,
                    defaultDispatcher = testDispatcher,
                    coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
                )

            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            viewModel.setFilter(FindingFilter.ALARMS)
            testDispatcher.scheduler.advanceUntilIdle()

            setContent {
                PcdaoAuditScreen(viewModel = viewModel, onBack = {})
            }

            onNode(hasScrollToIndexAction()).performScrollToNode(hasText(AppStringsPcdao.tptaFieldAdvisoryTitle))
            onNodeWithText(AppStringsPcdao.tptaFieldAdvisoryTitle).assertIsDisplayed()
            val dismissButton = onNodeWithTag(TestTags.DISMISS_ALARM_BUTTON)
            dismissButton.performClick()
            testDispatcher.scheduler.advanceUntilIdle()

            onNodeWithText(AppStringsPcdao.tptaFieldAdvisoryTitle).assertDoesNotExist()
            assertFalse(
                viewModel.uiState.value.filteredDiscrepancies.any {
                    it.id == AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE
                },
            )
            assertEquals(0, viewModel.uiState.value.alarmsCount)
        }

    private fun createAlarmDiscrepancy() =
        AuditDiscrepancy(
            id = AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE,
            title = AppStringsPcdao.tptaFieldAdvisoryTitle,
            type = DiscrepancyType.FORFEITURE_RISK,
            severity = DiscrepancySeverity.WARNING,
            drawnAmount = 7200.0,
            entitledAmount = 7200.0,
            netDue = 0.0,
            authority = "TR-230(B)",
            explanation = AppStringsPcdao.tptaFieldAdvisoryExplanation,
            recommendedAction = AppStringsPcdao.tptaFieldAdvisoryAction,
        )

    private fun createMockPayslip(
        dateStr: String,
        year: Int,
        monthNum: Int,
        basicPay: Double,
        tpta: Double,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "Month_$monthNum",
            dateStr = dateStr,
            officer = Officer("Col Test Officer", "01/142/987654", "ABCDE1234F"),
            earnings = Earnings(basicPay, basicPay * 0.5, 15500.0, transportAllowance = tpta),
            deductions = Deductions(dsopSubscription = 20000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(basicPay + basicPay * 0.5 + 15500.0 + tpta, 20000.0, basicPay),
            taxAndSavings = null,
        )

    private class MockUiAssetProvider : PcdaoAssetProvider {
        override suspend fun loadAsset(fileName: String): String = "{}"
    }
}
