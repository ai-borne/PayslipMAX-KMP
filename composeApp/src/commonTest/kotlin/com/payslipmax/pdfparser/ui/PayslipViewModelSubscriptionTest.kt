package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.isDebugBuild
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the ViewModel gating façade ([hasAccess]/[setDevOverride]) reflects both the premium flag
 * and the dev override for every [FeatureGate]. Assertions branch on [isDebugBuild] because the same
 * suite runs under both testDebugUnitTest (override active) and testReleaseUnitTest (override inert).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelSubscriptionTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultOverrideMatchesBuild() {
        val expected = if (isDebugBuild()) DevOverride.FORCE_PRO else DevOverride.FOLLOW_FLAG
        assertEquals(expected, viewModel.devOverride.value)
        if (isDebugBuild()) {
            for (gate in FeatureGate.values()) {
                assertTrue(viewModel.hasAccess(gate), "FORCE_PRO default must grant $gate in debug")
            }
        }
    }

    @Test
    fun testForceFreeBlocksEveryGateInDebugButIsInertInRelease() =
        runTest {
            viewModel.setPremiumEnabled(true)
            runCurrent()
            viewModel.setDevOverride(DevOverride.FORCE_FREE)

            assertTrue(viewModel.uiState.value.isPremiumEnabled)
            for (gate in FeatureGate.values()) {
                if (isDebugBuild()) {
                    assertFalse(viewModel.hasAccess(gate), "FORCE_FREE must block $gate even when premium")
                } else {
                    // Setter is inert in release: gating still follows the (premium) flag.
                    assertTrue(viewModel.hasAccess(gate), "release must ignore override and grant $gate when premium")
                }
            }
        }

    @Test
    fun testFollowFlagTracksPremiumFlagForEveryGate() =
        runTest {
            viewModel.setDevOverride(DevOverride.FOLLOW_FLAG)

            viewModel.setPremiumEnabled(false)
            runCurrent()
            for (gate in FeatureGate.values()) {
                assertFalse(viewModel.hasAccess(gate), "FOLLOW_FLAG + free must block $gate")
            }

            viewModel.setPremiumEnabled(true)
            runCurrent()
            for (gate in FeatureGate.values()) {
                assertTrue(viewModel.hasAccess(gate), "FOLLOW_FLAG + premium must grant $gate")
            }
        }
}
