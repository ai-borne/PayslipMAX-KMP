package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: lookup is free and drafting is Premium (Round 10), and a user whose Premium ended keeps what they made: they may open and delete
 * it, never create or change one (Round 13). Each test is one entitlement state; together they walk every way into a draft level and
 * prove none of them reaches the form without the entitlement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2DraftAccessUiTest : Part2DraftUiBase() {
    private fun assertNoForm() {
        composeRule.onAllNodesWithText("First day").assertCountEquals(0)
        composeRule.onAllNodesWithText(Part2Strings.seeSlip).assertCountEquals(0)
    }

    @Test
    fun aFreeUserSeesPremiumOnTheRowAndTheUpgradePanelAndNoDraftModelIsMade() {
        val nav = show(part2Unlocked = false)
        path()
        assertTrue(has(Part2Strings.draftPremium), "the row is marked Premium")
        draftRow()
        assertEquals(Part2Destination.Locked, nav.current)
        see(Part2Strings.lockedTitle).assertIsDisplayed()
        assertNoForm()
        tap(GuideStrings.unlock)
        assertEquals(1, unlockTaps)
        assertNull(nav.part2Draft)
        assertEquals(1, drafted, "no draft id was ever made")
    }

    @Test
    fun aFreeUserWhoRestoresIntoADraftLevelSeesTheUpgradePanelNotTheForm() {
        savedDraft("draft-3").also { runBlocking { repository.deleteDraft("draft-3") } }
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("draft-3"))), part2Unlocked = false)
        see(Part2Strings.lockedTitle).assertIsDisplayed()
        assertNoForm()
    }

    @Test
    fun aFreeUserWithNoDraftsOnASlipLevelSeesTheUpgradePanel() {
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip("draft-3"))), part2Unlocked = false)
        see(Part2Strings.lockedTitle).assertIsDisplayed()
        assertNoForm()
    }

    @Test
    fun aLapsedOfficerCanOpenTheirDraftButNeverTheForm() {
        savedDraft("draft-3")
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("draft-3"))), part2Unlocked = false)
        see(Part2Strings.lockedTitle).assertIsDisplayed()
        assertNoForm()
    }

    @Test
    fun aLapsedOfficerOpensADraftFromTheListAsAReadOnlySlip() {
        savedDraft("draft-3")
        val nav = show(part2Unlocked = false)
        tap(Part2Strings.entryTitle)
        see(Part2Strings.draftsHeading).assertIsDisplayed()
        tap(Part2Strings.draftRowTitle(listOf(GRANT)))
        assertEquals(Part2Destination.Slip("draft-3"), nav.current)
        see("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026").assertIsDisplayed()
        see(Part2Strings.viewOnlyNote).assertIsDisplayed()
        composeRule.onAllNodesWithText(Part2Strings.editDraft).assertCountEquals(0)
    }

    @Test
    fun aLapsedOfficerDeletesADraftFromTheListOnlyAfterConfirming() {
        savedDraft("draft-3")
        show(part2Unlocked = false)
        tap(Part2Strings.entryTitle)
        tap(Part2Strings.deleteDraft)
        see(Part2Strings.deleteTitle).assertIsDisplayed()
        tap(Part2Strings.deleteCancel)
        assertNotNull(runBlocking { repository.draft("draft-3") }, "cancel keeps it")
        tap(Part2Strings.deleteDraft)
        tap(Part2Strings.deleteConfirm)
        assertNull(runBlocking { repository.draft("draft-3") })
        composeRule.onAllNodesWithText(Part2Strings.draftsHeading).assertCountEquals(0)
    }

    @Test
    fun aLapsedOfficerDeletesFromTheSlipAndLandsBackOnPartTwoHome() {
        savedDraft("draft-3")
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip("draft-3"))), part2Unlocked = false)
        tap(Part2Strings.deleteDraft)
        tap(Part2Strings.deleteConfirm)
        assertNull(runBlocking { repository.draft("draft-3") })
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun aLapsedOfficerStillSeesPremiumOnTheDraftRowAndItOpensTheUpgradePanel() {
        savedDraft("draft-3")
        val nav = show(part2Unlocked = false)
        path()
        assertTrue(has(Part2Strings.draftPremium), "the row is marked Premium")
        draftRow()
        assertEquals(Part2Destination.Locked, nav.current)
    }

    @Test
    fun anEntitledOfficerHoldingDraftsSeesThemListedAndOpensOneOnItsSlip() {
        savedDraft("draft-3")
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap(Part2Strings.draftRowTitle(listOf(GRANT)))
        assertEquals(Part2Destination.Slip("draft-3"), nav.current)
        tap(Part2Strings.editDraft)
        assertEquals(Part2Destination.Draft("draft-3"), nav.current)
        composeRule.onNodeWithText("2026").assertIsDisplayed()
    }

    @Test
    fun anUnfinishedDraftOpenedOnItsSlipSaysWhatIsStillNeededInsteadOfDrawingARow() {
        val values = mapOf("leave_from" to com.payslipmax.pdfparser.part2.domain.SlotValue.DateValue(day(7, 5)))
        val request = com.payslipmax.pdfparser.part2.domain.OccurrenceRequest(GRANT, values)
        runBlocking { repository.saveDraft(com.payslipmax.pdfparser.part2.domain.Part2Draft("draft-4", com.payslipmax.pdfparser.part2.domain.LeaveEventInput(listOf(request)), emptyMap(), 1L, 2L)) }
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip("draft-4"))), part2Unlocked = false)
        see(Part2Strings.unfinishedLine(listOf("Last day", "Year"))).assertIsDisplayed()
        composeRule.onAllNodesWithText(Part2Strings.orderTitle(1, listOf("A")), substring = true).assertCountEquals(0)
    }
}
