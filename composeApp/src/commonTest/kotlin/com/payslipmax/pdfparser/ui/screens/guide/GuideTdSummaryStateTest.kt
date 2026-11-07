package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The TD summary card's state (plan 24, P4). Why it matters: owner decision D3 makes the whole card free, so a locked user must still
 * get the table and the authority; and the table's step must follow the officer's own DA once a payslip gives one.
 */
class GuideTdSummaryStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModel =
        GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideBundle.withTdSummary())), FakeCrashReporter(), dispatcher)
    private val profile = GuideProfile(2026, 6, level = "12A", basicPay = 144200.0, daPercent = 60, tptaClass = null, hraAmount = null)
    private val id = SyntheticGuideBundle.TD_SUMMARY_CARD

    private fun test(block: suspend TestScope.() -> Unit) =
        runTest(dispatcher) {
            viewModel.load()
            testScheduler.advanceUntilIdle()
            block()
        }

    @Test
    fun aLockedUserStillGetsTheTableAndTheWholeCardIncludingTheAuthority() =
        test {
            val card = assertNotNull(viewModel.card(id, unlocked = false, profile = null))

            assertNotNull(card.summary, "the table is free")
            assertNotNull(card.full, "the card's own text and authority are free too")
            assertEquals(false, card.full?.cite.isNullOrBlank())
        }

    @Test
    fun theOfficersLevelPicksTheirRowAndTheirDaPicksTheStep() =
        test {
            val card = assertNotNull(viewModel.card(id, unlocked = true, profile = profile))

            assertEquals("12A", card.ownLevel)
            assertEquals(50 to 99, assertNotNull(card.summary).let { it.daBandFrom to it.daBandTo })
            val past100 = assertNotNull(viewModel.card(id, unlocked = true, profile = profile.copy(daPercent = 100)))
            assertEquals(100 to 149, assertNotNull(past100.summary).let { it.daBandFrom to it.daBandTo })
        }

    @Test
    fun withoutAPayslipTheTableIsAtTheBundlesTierAndNoRowIsMarked() =
        test {
            val card = assertNotNull(viewModel.card(id, unlocked = true, profile = null))

            assertEquals(50 to 99, assertNotNull(card.summary).let { it.daBandFrom to it.daBandTo })
            assertNull(card.ownLevel)
        }

    @Test
    fun aProfileWithNoDaStillUsesTheBundlesTierButKeepsTheLevel() =
        test {
            val card = assertNotNull(viewModel.card(id, unlocked = true, profile = profile.copy(daPercent = null)))

            assertEquals(50, assertNotNull(card.summary).daBandFrom)
            assertEquals("12A", card.ownLevel)
        }

    @Test
    fun anOrdinaryCardHasNoTableAndAnOrdinaryLockedCardStaysLocked() =
        test {
            val unlocked = assertNotNull(viewModel.card("RB-T5", unlocked = true, profile = profile))
            assertNull(unlocked.summary)
            assertNull(unlocked.ownLevel)
            assertNull(viewModel.card("RB-T5", unlocked = false, profile = profile)?.full, "only the summary card is free")
        }

    @Test
    fun aCardWhoseSummaryKindThisVersionDoesNotKnowIsOrdinary() =
        test {
            val future = SyntheticGuideBundle.withTdSummary().let { b -> b.copy(cards = b.cards.map { if (it.id == id) it.copy(summary = "zz") else it }) }
            val vm = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(future)), FakeCrashReporter(), dispatcher)
            vm.load()
            testScheduler.advanceUntilIdle()

            val card = assertNotNull(vm.card(id, unlocked = false, profile = profile))
            assertNull(card.summary)
            assertNull(card.full, "an unknown kind is not free")
        }
}
