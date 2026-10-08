package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [GuideViewModel.openCard], the one entry point a Pay Audit finding uses to open a Guide card (E7). */
class GuideOpenCardTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGuideRepository()
    private val crashReporter = FakeCrashReporter()
    private val viewModel = GuideViewModel(repository, crashReporter, dispatcher)

    @Test
    fun noCardIsPendingUntilSomeoneOpensOne() {
        assertNull(viewModel.pendingCard.value)
    }

    @Test
    fun openCardRecordsTheTargetAndStartsTheLoadWithoutTheGuideTab() =
        runTest(dispatcher) {
            // The Pay Audit link can be the first thing that touches the Guide in a session.
            viewModel.openCard(SyntheticGuideBundle.PERSONAL_CARD)
            testScheduler.advanceUntilIdle()

            assertEquals(SyntheticGuideBundle.PERSONAL_CARD, viewModel.pendingCard.value)
            assertEquals(1, repository.loadCount)
            assertEquals("RB-T1", viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true)?.id)
        }

    @Test
    fun openingASecondCardReplacesTheTargetAndNeverReloadsTheBundle() =
        runTest(dispatcher) {
            viewModel.openCard("RB-T1")
            viewModel.openCard("RB-T2")
            testScheduler.advanceUntilIdle()

            assertEquals("RB-T2", viewModel.pendingCard.value)
            assertEquals(1, repository.loadCount)
        }

    @Test
    fun anIdTheBundleDoesNotHoldYieldsNoCardSoTheHostCanPopInsteadOfShowingABlankScreen() =
        runTest(dispatcher) {
            viewModel.openCard("RB-GONE")
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.card("RB-GONE", unlocked = true))
        }

    @Test
    fun openingACardReportsNothingToTelemetry() =
        runTest(dispatcher) {
            viewModel.openCard(SyntheticGuideBundle.PERSONAL_CARD)
            testScheduler.advanceUntilIdle()

            assertEquals(0, crashReporter.exceptions.size)
            assertEquals(emptyMap(), crashReporter.keys)
            assertEquals(emptyList(), crashReporter.logs)
        }

    @Test
    fun aLockedUserIsOfferedTheUpgradeAndNoCardIsEverOpened() =
        runTest(dispatcher) {
            var locked = 0
            var opened = 0

            openGuideCardFromFinding("RB-T5", unlocked = false, guide = viewModel, onLocked = { locked++ }, onOpened = { opened++ })
            testScheduler.advanceUntilIdle()

            assertEquals(1, locked)
            assertEquals(0, opened, "a locked user must never be taken to a card")
            assertNull(viewModel.pendingCard.value)
            assertEquals(0, repository.loadCount, "and the Guide is not even loaded for them")
        }

    @Test
    fun anUnlockedUserGoesThroughOpenCardThenNavigates() =
        runTest(dispatcher) {
            var locked = 0
            var opened = 0

            openGuideCardFromFinding("RB-T5", unlocked = true, guide = viewModel, onLocked = { locked++ }, onOpened = { opened++ })
            testScheduler.advanceUntilIdle()

            assertEquals(0, locked)
            assertEquals(1, opened)
            assertEquals("RB-T5", viewModel.pendingCard.value)
        }
}
