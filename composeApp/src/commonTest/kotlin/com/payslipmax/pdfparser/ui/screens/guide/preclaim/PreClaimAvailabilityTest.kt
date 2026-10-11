package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The kill switches (plan 22, P6): with the dark-launch flag off the entry is hidden for everyone, Premium or not; it also needs the
 * Guide itself; and once on, the Guide's own entitlement decides between the worksheet and the locked panel.
 */
class PreClaimAvailabilityTest {
    private val unlocked = GuideAccess(isUnlocked = true, onUnlock = {})
    private val locked = GuideAccess(isUnlocked = false, onUnlock = {})

    @Test
    fun theFlagOffHidesTheEntryForEveryoneEvenAUserWhoCouldOpenIt() {
        assertEquals(PreClaimEntry.HIDDEN, preClaimEntry(unlocked, enabled = isPreClaimEnabled(guideEnabled = true, preClaimEnabled = false)))
        assertEquals(PreClaimEntry.HIDDEN, preClaimEntry(locked, enabled = isPreClaimEnabled(guideEnabled = true, preClaimEnabled = false)))
    }

    @Test
    fun theWorksheetNeedsTheGuideTabItLivesIn() {
        assertFalse(isPreClaimEnabled(guideEnabled = false, preClaimEnabled = true))
        assertEquals(PreClaimEntry.HIDDEN, preClaimEntry(unlocked, enabled = isPreClaimEnabled(guideEnabled = false, preClaimEnabled = true)))
    }

    @Test
    fun withTheFlagOnTheGuidesEntitlementPicksTheWorksheetOrTheLockedPanel() {
        assertEquals(PreClaimEntry.OPEN, preClaimEntry(unlocked, enabled = true))
        assertEquals(PreClaimEntry.LOCKED, preClaimEntry(locked, enabled = true))
    }

    @Test
    fun theShippedDefaultIsTheFlagsAndNothingElse() {
        assertEquals(LaunchFlags.GUIDE_ENABLED && LaunchFlags.PRECLAIM_ENABLED, isPreClaimEnabled())
        assertEquals(PreClaimEntry.HIDDEN, preClaimEntry(unlocked), "dark in this build: no user reaches the worksheet")
    }
}
