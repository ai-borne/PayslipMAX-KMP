package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.domain.PersonalFigure
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase E6, the paid half: "your figure" is part of what Premium unlocks, so a locked card state must not hold a resolved
 * figure at all (not merely hide it), and a missing profile must leave the card complete. The personal card's key bullet
 * names placeholders ("Rs {food_rate}") that are never drawn raw.
 */
class GuideCardFigureStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val crashReporter = FakeCrashReporter()
    private val viewModel =
        GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideBundle.withFigures())), crashReporter, dispatcher)
    private val profile = GuideProfile(2026, 6, level = "14", basicPay = 144200.0, daPercent = 60, tptaClass = null, hraAmount = null)

    private fun test(block: suspend TestScope.() -> Unit) =
        runTest(dispatcher) {
            viewModel.load()
            testScheduler.advanceUntilIdle()
            block()
        }

    @Test
    fun anUnlockedFigureCardCarriesItsResolvedFigure() =
        test {
            val full = assertNotNull(viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true, profile = profile)?.full)

            val figure = assertIs<PersonalFigure.FoodRate>(full.figure)
            assertEquals(1500L, figure.amount, "level 14 at DA 60%: 1,200 + 25%")
        }

    @Test
    fun aLockedCardStateHoldsNoFigureEvenWhenAProfileIsSupplied() =
        test {
            val card = assertNotNull(viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false, profile = profile))

            assertNull(card.full, "the whole paid half is absent, so no screen can draw the figure by mistake")
        }

    @Test
    fun withoutAProfileTheCardIsCompleteAndHasNoFigure() =
        test {
            val full = assertNotNull(viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true, profile = null)?.full)

            assertNull(full.figure)
            assertEquals(emptyList(), full.body.key, "the only key bullet is the placeholder one, kept out of sight")
            assertEquals(listOf("A form"), full.body.attach)
            assertTrue(full.cite.isNotBlank() && full.details.isNotBlank())
        }

    @Test
    fun aProfileTooThinToSettleTheFigureLeavesTheCardAsItIs() =
        test {
            val noLevel = profile.copy(level = null)

            assertNull(viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true, profile = noLevel)?.full?.figure)
        }

    @Test
    fun aCardWithoutAFigureResolvesNothing() =
        test {
            assertNull(viewModel.card("RB-T5", unlocked = true, profile = profile)?.full?.figure)
        }

    @Test
    fun onlyACardWithAFigureAsksForTheProfile() =
        test {
            assertTrue(viewModel.hasFigure(SyntheticGuideBundle.PERSONAL_CARD))
            assertEquals(false, viewModel.hasFigure("RB-T5"))
            assertEquals(false, viewModel.hasFigure("RB-NOPE"))
        }

    @Test
    fun resolvingAFigureReportsNothingToTelemetry() =
        test {
            viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true, profile = profile)
            viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false, profile = profile)

            assertTrue(
                crashReporter.logs.isEmpty() && crashReporter.keys.isEmpty() && crashReporter.exceptions.isEmpty(),
                "a figure is computed on the device and never leaves it",
            )
        }
}
