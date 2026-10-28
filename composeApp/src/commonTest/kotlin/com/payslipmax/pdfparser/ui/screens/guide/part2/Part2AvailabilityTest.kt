package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.testing.Part2FlagExpectation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The kill switches (Part II plan, phase 1): Part II exists only when its own dark-launch flag is on and the Guide it lives in is on.
 * Off, the tile, the routes and the Koin module are absent and a flag-off build behaves exactly as before.
 */
class Part2AvailabilityTest {
    @Test
    fun partTwoNeedsBothItsOwnFlagAndTheGuide() {
        assertTrue(isPart2Enabled(guideEnabled = true, part2Enabled = true))
        assertFalse(isPart2Enabled(guideEnabled = true, part2Enabled = false), "the dark-launch flag alone decides while the Guide is on")
        assertFalse(isPart2Enabled(guideEnabled = false, part2Enabled = true), "Part II lives in the Guide tab, so the Guide kill switch wins")
        assertFalse(isPart2Enabled(guideEnabled = false, part2Enabled = false))
    }

    @Test
    fun theShippedDefaultIsTheFlagsAndNothingElse() {
        assertEquals(LaunchFlags.GUIDE_ENABLED && LaunchFlags.PART2_ENABLED, isPart2Enabled())
    }

    @Test
    fun theFlagIsWhereTheCommittedExpectationSaysAndOnlyAFlipCommitMovesBoth() {
        // why: a flag changed by accident (a stray edit, a merge) must fail here, while the deliberate flip commit edits LaunchFlags.kt and
        // Part2FlagExpectation together. scripts/run_flag_on_tests.py flips both in a throwaway copy, so a flag-on run has no expected failure.
        assertEquals(Part2FlagExpectation.PART2_ENABLED, LaunchFlags.PART2_ENABLED, "flip LaunchFlags.kt and Part2FlagExpectation.kt together")
        assertEquals(Part2FlagExpectation.PART2_ENABLED && LaunchFlags.GUIDE_ENABLED, isPart2Enabled())
    }
}
