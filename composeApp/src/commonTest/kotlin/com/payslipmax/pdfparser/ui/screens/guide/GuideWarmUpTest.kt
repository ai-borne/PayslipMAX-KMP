package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Why: the Guide parses a ~400 KB bundle on its first load, so opening the tab cold showed a spinner. The app warms
 * it up shortly after launch; it must wait (so it never competes with the first screen) and must not reload.
 */
class GuideWarmUpTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGuideRepository()
    private val viewModel = GuideViewModel(repository, FakeCrashReporter(), dispatcher)

    @Test
    fun warmUpWaitsForTheStartDelayBeforeReadingTheBundle() =
        runTest(dispatcher) {
            launch { warmUpGuide(viewModel, startDelayMs = 1_000) }
            testScheduler.advanceTimeBy(999)
            testScheduler.runCurrent()
            assertEquals(0, repository.loadCount)

            testScheduler.advanceTimeBy(2)
            testScheduler.advanceUntilIdle()
            assertEquals(1, repository.loadCount)
        }

    @Test
    fun theGuideIsReadyBeforeTheTabIsFirstOpened() =
        runTest(dispatcher) {
            warmUpGuide(viewModel, startDelayMs = 0)
            testScheduler.advanceUntilIdle()
            assertIs<GuideUiState.Ready>(viewModel.uiState.value)
        }

    @Test
    fun openingTheTabAfterWarmUpDoesNotReadTheBundleAgain() =
        runTest(dispatcher) {
            warmUpGuide(viewModel, startDelayMs = 0)
            testScheduler.advanceUntilIdle()
            viewModel.load() // what GuideTab does on first open
            testScheduler.advanceUntilIdle()
            assertEquals(1, repository.loadCount)
        }
}
