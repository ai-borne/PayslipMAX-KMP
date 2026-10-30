package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.testing.Part2FlagExpectation
import com.payslipmax.pdfparser.ui.screens.premiumFeatureCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: Part II is dark-launched. With the flag off the app must be exactly the app that is in the stores, and the flag must not move by
 * accident (a stray edit, a merge), while the deliberate flip commit edits the flag and its expectation together.
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
        // scripts/run_flag_on_tests.py flips both in a throwaway copy, so a flag-on run has no expected failure.
        assertEquals(Part2FlagExpectation.PART2_ENABLED, LaunchFlags.PART2_ENABLED, "flip LaunchFlags.kt and Part2FlagExpectation.kt together")
    }

    @Test
    fun withTheFlagOffThePremiumCatalogIsExactlyTheOneInTheStores() {
        // The 1.5.0 catalog, by name. A Part II row must arrive only behind the flag, so this list is the flag-off app.
        val shipped =
            listOf(
                "PREMIUM_INTELLIGENCE", "WEALTH_OPTIMIZATION", "TAX_PLANNER", "DSOP_SIMULATOR", "ANOMALY_DETECTION",
                "CLAIM_GENERATOR", "RETIREMENT_CALCULATORS", "BACKUP_RESTORE", "CLAIM_GUIDE",
            )
        if (!isPart2Enabled()) assertEquals(shipped, premiumFeatureCatalog().map { it.gate.name })
    }
}
