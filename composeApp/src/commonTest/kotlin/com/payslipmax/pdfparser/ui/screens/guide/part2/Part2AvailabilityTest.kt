package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.subscription.LaunchFlags
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
    fun theFlagShipsOffUntilTheOwnerFlipsItInPhase10() {
        // why: Part II is built over many phases; a flag-off build must behave exactly as before until the owner's content sign-off.
        assertFalse(LaunchFlags.PART2_ENABLED)
        assertFalse(isPart2Enabled())
    }
}
