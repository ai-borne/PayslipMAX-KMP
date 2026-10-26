package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.isDebugBuild
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the reminder sync outlives every screen and cannot ask Compose, yet "reminders are Premium" must be decided by the one entitlement
 * authority (`hasAccess`). So the entitlement is also a flow of that same verdict: it must follow the premium flag and, in debug, the
 * override, and say nothing when neither changes the verdict. Branches on [isDebugBuild] because the suite runs under both variants.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelAccessFlowTest {
    private val dispatcher = UnconfinedTestDispatcher()
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

    @Test
    fun theFlowFollowsThePremiumFlagAndAnswersWhatHasAccessAnswers() =
        runTest(dispatcher) {
            viewModel.setPremiumEnabled(false)
            runCurrent()
            val seen = mutableListOf<Boolean>()
            val job = launch { viewModel.hasAccessFlow(FeatureGate.PART2_ORDERS).collect { seen += it } }
            runCurrent()

            viewModel.setPremiumEnabled(true)
            runCurrent()
            viewModel.setPremiumEnabled(false)
            runCurrent()
            job.cancel()

            assertEquals(listOf(false, true, false), seen)
            assertEquals(viewModel.hasAccess(FeatureGate.PART2_ORDERS), seen.last())
        }

    @Test
    fun theFlowFollowsTheDebugOverrideAndIsInertInRelease() =
        runTest(dispatcher) {
            viewModel.setPremiumEnabled(false)
            runCurrent()
            val seen = mutableListOf<Boolean>()
            val job = launch { viewModel.hasAccessFlow(FeatureGate.PART2_ORDERS).collect { seen += it } }
            runCurrent()

            viewModel.setDevOverride(DevOverride.FORCE_PRO)
            runCurrent()
            viewModel.setDevOverride(DevOverride.FORCE_FREE)
            runCurrent()
            job.cancel()

            assertEquals(if (isDebugBuild()) listOf(false, true, false) else listOf(false), seen)
        }

    @Test
    fun aChangeThatLeavesTheVerdictAloneEmitsNothing() =
        runTest(dispatcher) {
            viewModel.setPremiumEnabled(true)
            runCurrent()
            val seen = mutableListOf<Boolean>()
            val job = launch { viewModel.hasAccessFlow(FeatureGate.PART2_ORDERS).collect { seen += it } }
            runCurrent()

            viewModel.setDevOverride(DevOverride.FOLLOW_FLAG)
            viewModel.setDevOverride(DevOverride.FOLLOW_FLAG)
            runCurrent()
            job.cancel()

            assertEquals(listOf(true), seen)
        }

    @Test
    fun aChangeElsewhereInTheStateThatLeavesTheVerdictAloneEmitsNothing() =
        runTest(dispatcher) {
            viewModel.setPremiumEnabled(true)
            runCurrent()
            val seen = mutableListOf<Boolean>()
            val job = launch { viewModel.hasAccessFlow(FeatureGate.PART2_ORDERS).collect { seen += it } }
            runCurrent()

            viewModel.setAppTheme("dark")
            runCurrent()
            viewModel.setAppTheme("light")
            runCurrent()
            job.cancel()

            assertEquals(listOf(true), seen, "the screen state changed twice and the entitlement did not")
        }
}
