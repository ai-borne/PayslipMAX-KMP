package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: lookup is free, drafting is Premium, and a user whose Premium ended keeps their drafts (Round 10 and 13): they can open and delete
 * what they made, never create or change one. One function decides, so no screen re-derives it.
 */
class Part2DraftEntryTest {
    @Test
    fun anEntitledUserDraftsWhetherOrNotTheyHoldDrafts() {
        assertEquals(Part2DraftEntry.OPEN, part2DraftEntry(unlocked = true, hasDrafts = false))
        assertEquals(Part2DraftEntry.OPEN, part2DraftEntry(unlocked = true, hasDrafts = true))
    }

    @Test
    fun aUserWhosePremiumEndedButWhoHoldsDraftsCanOnlyViewAndDelete() {
        assertEquals(Part2DraftEntry.VIEW_ONLY, part2DraftEntry(unlocked = false, hasDrafts = true))
    }

    @Test
    fun aFreeUserWithNoDraftsSeesTheLockedPanel() {
        assertEquals(Part2DraftEntry.LOCKED, part2DraftEntry(unlocked = false, hasDrafts = false))
    }

    @Test
    fun theThreeStatesAreTheOnlyOnes() {
        assertEquals(listOf(Part2DraftEntry.LOCKED, Part2DraftEntry.VIEW_ONLY, Part2DraftEntry.OPEN), Part2DraftEntry.entries)
    }

    @Test
    fun partTwoAccessFollowsTheGuidesUntilTheAppSaysOtherwise() {
        assertTrue(GuideAccess(true) {}.part2Unlocked)
        assertFalse(GuideAccess(false) {}.part2Unlocked)
        assertFalse(GuideAccess(isUnlocked = true, part2Unlocked = false, onUnlock = {}).part2Unlocked, "drafting has its own gate: Guide access does not open it")
        assertTrue(GuideAccess(isUnlocked = false, part2Unlocked = true, onUnlock = {}).part2Unlocked)
    }
}
