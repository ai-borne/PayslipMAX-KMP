package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The pickers offer only what the calculators accept: levels as the rate bands print them and states as the RMA circular spells
 * them. A hand-typed level or state that nearly matches is refused by the calculators, so a picker built from the figures is the
 * only way an officer reaches an estimate.
 */
class PreClaimChoicesTest {
    private val figures = SyntheticGuideFigures.figures

    @Test
    fun levelsAreEveryLevelAPreClaimBandCoversLowestFirstWithNoRepeats() {
        val levels = PreClaimChoices.levels(figures)

        assertEquals(listOf("9", "10", "10A", "10B", "11", "12", "12A", "12B", "13", "13A", "13B", "14", "15", "16", "17", "18"), levels)
        assertEquals(levels.size, levels.toSet().size)
    }

    @Test
    fun everyOfferedLevelIsOneTheLodgingCeilingAcceptsSoNoPickerChoiceHidesTheEstimate() {
        for (level in PreClaimChoices.levels(figures)) {
            val profile = PreClaimProfile(level, 100_000, 50, null, null)
            val outcome = TdLodgingCalculator.estimate(figures, profile, days = 2, nights = 1, nightlyRateRupees = 1000)
            assertTrue(outcome is LodgingEstimate, "level $level is offered but the ceiling does not cover it")
        }
    }

    @Test
    fun statesAreTheRmaTablesOwnSpellingAlphabeticallyAndEveryOneIsPriced() {
        val states = PreClaimChoices.states(figures)

        assertEquals(34, states.size)
        assertEquals(states.sorted(), states)
        assertTrue("Gujrat" in states && "Gujarat" !in states, "the circular's spelling, so a near miss cannot be offered")
        for (state in states) {
            assertTrue(CarConveyanceCalculator.estimate(figures, state, 100) is PerKmEstimate, "state $state is offered but unpriced")
        }
    }

    @Test
    fun noFiguresMeansNothingToPick() {
        assertEquals(emptyList(), PreClaimChoices.levels(null))
        assertEquals(emptyList(), PreClaimChoices.states(null))
    }
}
