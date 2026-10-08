package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase E8: Pin, Copy cite and Share as claim note. The platform copy and share calls are recorded, so a test can prove
 * that nothing is copied or shared until a tap, and that the copied text is exactly the cite. A tall screen keeps every
 * section composed, so "does not exist" means left out, not scrolled off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuidePinsShareUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val storage = FakeGuidePinsStorage()
    private val copied = mutableListOf<String>()
    private val shared = mutableListOf<Pair<String, String>>()
    private val platform = GuidePlatform(copy = { copied += it }, share = { text, title -> shared += text to title })

    private fun title(id: String) = "Synthetic card $id?"

    private fun show(unlocked: Boolean): GuideNavState {
        val pins = GuidePinsModel(storage)
        val guide = GuideViewModel(FakeGuideRepository(), crash, dispatcher, pins = pins)
        val search = GuideSearchViewModel(guide, dispatcher)
        val nav = GuideNavState()
        val access = GuideAccess(unlocked, onUnlock = {})
        composeRule.setContent { GuideTab(navState = nav, access = access, viewModel = guide, searchViewModel = search, platform = platform) }
        settle()
        return nav
    }

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun openCard(
        id: String,
        unlocked: Boolean = true,
    ): GuideNavState {
        val nav = show(unlocked)
        tap("Travel")
        tap("Daily allowance on duty")
        tap(title(id))
        return nav
    }

    @Test
    fun anUnlockedCardOffersPinShareAndCopyCite() {
        openCard("RB-T5")

        composeRule.onNodeWithText(GuideStrings.pin).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.share).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.copyCite).assertIsDisplayed()
    }

    @Test
    fun aLockedCardHasNoPinShareOrCopyCiteButKeepsTheUnlockPanel() {
        openCard("RB-T5", unlocked = false)

        for (control in listOf(GuideStrings.pin, GuideStrings.unpin, GuideStrings.share, GuideStrings.copyCite)) {
            composeRule.onNodeWithText(control).assertDoesNotExist()
        }
        composeRule.onNodeWithText(GuideStrings.unlock).assertIsDisplayed()
    }

    @Test
    fun nothingIsCopiedOrSharedUntilATap() {
        openCard("RB-T5")

        assertTrue(copied.isEmpty() && shared.isEmpty())
    }

    @Test
    fun copyCiteCopiesExactlyTheCiteAndSharesNothing() {
        openCard("RB-T5")

        tap(GuideStrings.copyCite)

        assertEquals(listOf("Rule 114 TR"), copied)
        assertTrue(shared.isEmpty())
        composeRule.onNodeWithText(GuideStrings.citeCopied).assertIsDisplayed()
    }

    @Test
    fun shareSendsTheClaimNoteWithTheChooserTitleAndCopiesNothing() {
        openCard("RB-T5")

        tap(GuideStrings.share)

        val expected =
            "Claim note\n\nSynthetic card RB-T5?\n\nAnswer: A one-line answer for RB-T5.\n\n" +
                "Key points:\n- A short key point for RB-T5\n\nAttach:\n- A form\n\nAuthority: Rule 114 TR"
        assertEquals(listOf(expected to GuideStrings.shareChooserTitle), shared)
        assertTrue(copied.isEmpty())
    }

    @Test
    fun aCardWithNoCiteOffersNoCopyCiteButStillShares() {
        openCard(SyntheticGuideBundle.NO_CITE_CARD)

        composeRule.onNodeWithText(GuideStrings.copyCite).assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.share).assertIsDisplayed()
    }

    @Test
    fun pinningIsSavedTheButtonFlipsAndHomeListsThePinnedCard() {
        val nav = openCard("RB-T5")

        tap(GuideStrings.pin)

        composeRule.onNodeWithText(GuideStrings.unpin).assertIsDisplayed()
        assertEquals("RB-T5", storage.stored)
        composeRule.runOnUiThread { nav.popToHome() }
        settle()
        composeRule.onNodeWithText(GuideStrings.pinnedSection).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T5")).assertIsDisplayed()
    }

    @Test
    fun tappingAPinnedRowOnHomeOpensItsCard() {
        storage.stored = "RB-P2"
        val nav = show(unlocked = true)

        tap(title("RB-P2"))

        assertEquals(GuideDestination.Card("RB-P2"), nav.current)
    }

    @Test
    fun homeShowsNoPinnedSectionWhenNothingIsPinned() {
        show(unlocked = true)

        composeRule.onNodeWithText(GuideStrings.pinnedSection).assertDoesNotExist()
        composeRule.onNodeWithText("Travel").assertIsDisplayed()
    }

    @Test
    fun aLockedUserSeesNoPinnedSectionEvenWithPinsStored() {
        storage.stored = "RB-P2"
        show(unlocked = false)

        composeRule.onNodeWithText(GuideStrings.pinnedSection).assertDoesNotExist()
        composeRule.onNodeWithText(title("RB-P2")).assertDoesNotExist()
    }

    @Test
    fun aPinForACardTheBundleLostIsNotShownAndIsRemovedFromStorage() {
        storage.stored = "RB-GONE\nRB-P2"
        show(unlocked = true)

        composeRule.onNodeWithText(title("RB-P2")).assertIsDisplayed()
        assertEquals("RB-P2", storage.stored)
    }

    @Test
    fun newestPinIsListedFirstOnHome() {
        storage.stored = "RB-P1\nRB-P3"
        show(unlocked = true)

        val top = { id: String -> composeRule.onNodeWithText(title(id)).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(top("RB-P3") < top("RB-P1"))
    }

    @Test
    fun pinningCopyingAndSharingTellTelemetryNothing() {
        openCard("RB-T5")

        tap(GuideStrings.pin)
        tap(GuideStrings.copyCite)
        tap(GuideStrings.share)

        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty())
    }
}
