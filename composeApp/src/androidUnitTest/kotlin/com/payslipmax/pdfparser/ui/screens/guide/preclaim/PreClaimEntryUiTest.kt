package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.testing.SyntheticGuidePreClaim
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuideTab
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Plan 22, P7: how the worksheet is reached and left. The kill switch (flag off means no entry anywhere), the Premium gate (a free user
 * sees the upgrade panel and nothing about their pay is read), back, the tab re-tap, the lock/tab-switch discard, and the card button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class PreClaimEntryUiTest : PreClaimUiBase() {
    private fun title(id: String) = "Synthetic card $id?"

    @Test
    fun withTheFlagOffGuideHomeShowsNoEntryAndNoWorksheetIsEverMade() {
        show(preClaim = null)
        composeRule.onAllNodesWithText(PreClaimStrings.entryTitle).assertCountEquals(0)
        assertTrue(worksheets.isEmpty())
    }

    @Test
    fun withTheFlagOffAStackRestoredOntoTheWorksheetIsCutBackToHome() {
        val nav = show(GuideNavState(listOf(GuideDestination.PreClaim("PD"))), preClaim = null)
        assertEquals(GuideDestination.Home, nav.current)
        assertTrue(worksheets.isEmpty())
    }

    @Test
    fun anUnlockedUserOpensTheMoveChooserFromGuideHome() {
        show()
        tap(PreClaimStrings.entryTitle)
        composeRule.onNodeWithText(PreClaimStrings.chooseMove).assertIsDisplayed()
        listOf("Permanent duty", "Temporary duty", "Leave travel").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        assertEquals(1, worksheets.size)
    }

    @Test
    fun aFreeUserSeesThePremiumRowThenTheUpgradePanelAndNoWorksheetOrPayslipRead() {
        show(unlocked = false)
        composeRule.onNodeWithText(PreClaimStrings.entryPremium).assertIsDisplayed()
        tap(PreClaimStrings.entryTitle)
        composeRule.onNodeWithText(PreClaimStrings.lockedTitle).assertIsDisplayed()
        composeRule.onAllNodesWithText(PreClaimStrings.chooseMove).assertCountEquals(0)
        tap(GuideStrings.unlock)
        assertEquals(1, unlockTaps)
        assertTrue(worksheets.isEmpty(), "no worksheet is built for a free user")
        assertEquals(0, payslips.subscriptions, "a free user's payslips are never read")
    }

    @Test
    fun theBackArrowReturnsToGuideHome() {
        val nav = show()
        tap(PreClaimStrings.entryTitle)
        tapDescribed(AppStrings.btnBack)
        assertEquals(GuideDestination.Home, nav.current)
        composeRule.onNodeWithText(PreClaimStrings.entryTitle).assertIsDisplayed()
    }

    @Test
    fun reTappingTheTabGoesHomeAndTheTypedFormIsDiscarded() {
        val nav = show()
        openFromHome("Permanent duty")
        type(PreClaimStrings.roadKm, "2881")
        val worksheet = worksheets.single()
        assertEquals("2881", worksheet.typed.value.roadKm)
        nav.popToHome()
        settle()
        composeRule.onNodeWithText(PreClaimStrings.entryTitle).assertIsDisplayed()
        assertEquals(PreClaimFormState(), worksheet.typed.value, "leaving the worksheet empties its form")
    }

    @Test
    fun switchingTabsKeepsTheFormAndALockDiscardsIt() {
        var shown by mutableStateOf(true)
        val nav = GuideNavState()
        composeRule.setContent { if (shown) GuideTab(nav, GuideAccess(true) {}, guide, search, platform, null, factory) }
        settle()
        openFromHome("Permanent duty")
        type(PreClaimStrings.roadKm, "2881")
        val first = worksheets.single()
        shown = false
        settle()
        shown = true
        settle()
        assertEquals(1, worksheets.size, "a tab switch brings back the same worksheet")
        assertEquals("2881", first.typed.value.roadKm)
        composeRule.onNodeWithText("Composite transfer grant").assertIsDisplayed()
        shown = false
        settle()
        nav.discardPreClaim() // what App does when the app locks
        assertEquals(PreClaimFormState(), first.typed.value, "the lock emptied the form")
        shown = true
        settle()
        assertEquals(2, worksheets.size, "unlocking builds a new worksheet")
        composeRule.onNodeWithText(PreClaimStrings.chooseMove).assertIsDisplayed()
        assertEquals("", worksheets.last().typed.value.roadKm)
    }

    @Test
    fun aRelatedCardOffersPrepareThisClaimAndOpensTheWorksheetOnItsMove() {
        show(GuideNavState(listOf(GuideDestination.Card(SyntheticGuidePreClaim.PICKER_CARD))))
        tap(PreClaimStrings.prepareThisClaim)
        composeRule.onNodeWithText("Leave travel").assertIsDisplayed()
        composeRule.onNodeWithText(PreClaimStrings.changeMove).assertIsDisplayed()
        assertEquals(PreClaimMoveType.LEAVE_TRAVEL, worksheets.single().typed.value.move)
    }

    @Test
    fun anUnrelatedCardOffersNoPrepareButton() {
        show(GuideNavState(listOf(GuideDestination.Card("RB-T1"))))
        composeRule.onNodeWithText(title("RB-T1")).assertIsDisplayed()
        composeRule.onAllNodesWithText(PreClaimStrings.prepareThisClaim).assertCountEquals(0)
    }

    @Test
    fun withTheFlagOffEvenARelatedCardOffersNoPrepareButton() {
        show(GuideNavState(listOf(GuideDestination.Card(SyntheticGuidePreClaim.PICKER_CARD))), preClaim = null)
        composeRule.onAllNodesWithText(PreClaimStrings.prepareThisClaim).assertCountEquals(0)
    }

    @Test
    fun aFreeUserOnARelatedCardCanTapPrepareAndLandsOnTheUpgradePanel() {
        show(GuideNavState(listOf(GuideDestination.Card(SyntheticGuidePreClaim.PICKER_CARD))), unlocked = false)
        tap(PreClaimStrings.prepareThisClaim)
        composeRule.onNodeWithText(PreClaimStrings.lockedTitle).assertIsDisplayed()
        assertTrue(worksheets.isEmpty())
    }

    @Test
    fun readingTheRuleCardFromTheWorksheetKeepsWhatWasTypedAndBackReturnsToIt() {
        val nav = show()
        openFromHome("Leave travel")
        tap(PreClaimStrings.trRule + ": " + PreClaimStrings.trRuleChoose)
        tap("Home town")
        tap(PreClaimStrings.readRuleCard)
        assertEquals(GuideDestination.Card(SyntheticGuidePreClaim.PICKER_CARD), nav.current)
        composeRule.onAllNodesWithText(PreClaimStrings.prepareThisClaim).assertCountEquals(0)
        tapDescribed(AppStrings.btnBack)
        composeRule.onNodeWithText(PreClaimStrings.trRule + ": Home town").assertIsDisplayed()
        assertEquals(1, worksheets.size, "the same worksheet came back")
    }
}
