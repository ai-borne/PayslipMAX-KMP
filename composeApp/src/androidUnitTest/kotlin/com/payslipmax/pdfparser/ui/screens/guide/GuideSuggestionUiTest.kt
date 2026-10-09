package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.domain.GuideSuggestion
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
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
 * M5 on screen: "Suggest a correction" on an unlocked card opens a dialog, and Open email hands the allow-listed message to
 * the mail seam exactly once. Nothing is sent by the app, a locked card has no such action, cancelling discards the text,
 * and telemetry hears nothing. The mail seam records what would reach the user's mail app, and when.
 *
 * No `qualifiers` on purpose: with a `w360dp-...` screen the dialog's text layout never settles under Robolectric (the test
 * JVM spins until it runs out of memory), while the default screen is fine. The cards are opened on their own (the actions
 * row is at the top), so the short default screen is enough; the other Guide screen tests need a tall one only for feeds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideSuggestionUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val mails = mutableListOf<Triple<String, String, String>>()
    private val platform = GuidePlatform(copy = {}, share = { _, _ -> }, email = { to, subject, body -> mails += Triple(to, subject, body) })
    private val open = GuideMaintenanceStrings.suggestOpenEmail

    private fun model(result: GuideLoadResult = GuideLoadResult.Loaded(SyntheticGuideBundle.withFigures())) =
        GuideViewModel(FakeGuideRepository(result), crash, dispatcher, pins = GuidePinsModel(FakeGuidePinsStorage()), appVersion = { "9.9.9" })

    private fun showTab(guide: GuideViewModel) {
        val search = GuideSearchViewModel(guide, dispatcher)
        composeRule.setContent { GuideTab(GuideNavState(), GuideAccess(true, onUnlock = {}), guide, search, platform) }
        settle()
    }

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    /** The card on its own, as Pay Audit hosts it: the actions row sits at the top, so a short screen is enough. */
    private fun openCard(
        id: String = "RB-T5",
        unlocked: Boolean = true,
        guide: GuideViewModel = model(),
    ) {
        guide.openCard(id)
        composeRule.setContent { GuideCardHost(GuideAccess(unlocked, onUnlock = {}), onBack = {}, viewModel = guide, platform = platform) }
        settle()
    }

    /** The same card reached the way a user does in the Guide tab: a pinned row on Home (a feed would need a tall screen). */
    private fun openPinnedInTab(
        id: String,
        guide: GuideViewModel = model(),
    ) {
        guide.pins.toggle(id)
        showTab(guide = guide)
        tap("Synthetic card $id?")
    }

    private fun type(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        composeRule.waitForIdle()
    }

    @Test
    fun anUnlockedCardOffersSuggestNextToPinAndShareAndOpensNothingYet() {
        openCard()

        for (control in listOf(GuideStrings.pin, GuideStrings.share, GuideMaintenanceStrings.suggest)) composeRule.onNodeWithText(control).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.suggestNotice).assertDoesNotExist()
        assertTrue(mails.isEmpty())
    }

    @Test
    fun aLockedCardHasNoSuggestActionAndNoDialogAndKeepsTheUnlockPanel() {
        openCard(unlocked = false)

        composeRule.onNodeWithText(GuideMaintenanceStrings.suggest).assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.unlock).assertIsDisplayed()
        assertTrue(mails.isEmpty())
    }

    @Test
    fun tappingSuggestShowsTheDialogWithItsNoticeAndSendsNothing() {
        openCard()

        tap(GuideMaintenanceStrings.suggest)

        composeRule.onNodeWithText(GuideMaintenanceStrings.suggestNotice).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.suggestFieldLabel).assertIsDisplayed()
        composeRule.onNodeWithText(open).assertIsNotEnabled()
        assertTrue(mails.isEmpty(), "opening the dialog must not open the mail app")
    }

    @Test
    fun openEmailStaysOffForEmptyAndBlankTextAndComesOnWithRealText() {
        openCard()
        tap(GuideMaintenanceStrings.suggest)

        type("  \n ")
        composeRule.onNodeWithText(open).assertIsNotEnabled()
        type("x")
        composeRule.onNodeWithText(open).assertIsEnabled()
    }

    @Test
    fun openEmailHandsSupportTheAllowListedMessageOnceAndClosesTheDialog() {
        openCard()
        tap(GuideMaintenanceStrings.suggest)

        type("The rate is wrong.")
        tap(open)

        val (to, subject, body) = mails.single()
        assertEquals(AppStringsSupport.supportEmail, to)
        assertEquals("[Guide] RB-T5", subject)
        assertEquals(
            "Guide correction suggestion\n\nCard: RB-T5\nTitle: Synthetic card RB-T5?\nGuide data: 2026-10-07\n" +
                "App version: 9.9.9\n\nSuggestion:\nThe rate is wrong.",
            body,
        )
        composeRule.onNodeWithText(GuideMaintenanceStrings.suggestNotice).assertDoesNotExist()
        composeRule.onNodeWithText(GuideMaintenanceStrings.suggest).assertIsDisplayed()
    }

    @Test
    fun aLongTextIsCappedAtTheLimitInTheEmail() {
        openCard()
        tap(GuideMaintenanceStrings.suggest)
        val sentence = "The order letter was amended. "

        type(sentence.repeat(60))
        tap(open)

        val expected = sentence.repeat(60).take(GuideSuggestion.MAX_TEXT_LENGTH).trim()
        assertEquals(expected, mails.single().third.substringAfter("Suggestion:\n"))
        assertTrue(expected.length in 1 until sentence.length * 60, "the text typed was longer than the cap, and the email holds the capped part")
    }

    @Test
    fun specialCharactersReachTheEmailUnchanged() {
        openCard()
        tap(GuideMaintenanceStrings.suggest)
        val text = "Rs 1,500 & \"x\" <b>1</b> 100% = ₹1,500 + #1? रिपोर्ट"

        type(text)
        tap(open)

        assertEquals(text, mails.single().third.substringAfter("Suggestion:\n"))
    }

    @Test
    fun cancellingSendsNothingAndTheHalfTypedTextIsGoneWhenTheDialogOpensAgain() {
        openCard()
        tap(GuideMaintenanceStrings.suggest)
        type("half typed")

        tap(AppStrings.btnCancel)
        tap(GuideMaintenanceStrings.suggest)

        assertTrue(mails.isEmpty())
        composeRule.onNodeWithText("half typed").assertDoesNotExist()
        composeRule.onNodeWithText(open).assertIsNotEnabled()
    }

    @Test
    fun aCardWithAFigureLineStillSendsNoFigureOrCardText() {
        openCard(SyntheticGuideBundle.PERSONAL_CARD)
        tap(GuideMaintenanceStrings.suggest)

        type("Check this one.")
        tap(open)

        val body = mails.single().third
        assertTrue(listOf("A short key point", "Rule 114 TR", "Longer details", "1500", "1,500").none { body.contains(it) })
    }

    @Test
    fun aReplacedCardCanBeSuggestedAboutAndNamesItsOwnId() {
        val guide = model(GuideLoadResult.Loaded(SyntheticGuideRuleChange.bundle()))
        openPinnedInTab(SyntheticGuideRuleChange.OLD_CARD, guide)
        tap(GuideMaintenanceStrings.suggest)

        type("This is still cited.")
        tap(open)

        assertEquals("[Guide] ${SyntheticGuideRuleChange.OLD_CARD}", mails.single().second)
    }

    @Test
    fun suggestingTellsTelemetryNothing() {
        openCard()
        tap(GuideMaintenanceStrings.suggest)
        type("The rate is wrong.")
        tap(open)

        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty())
    }

    @Test
    fun theGuideTabOffersItToo() {
        openPinnedInTab("RB-T5")
        tap(GuideMaintenanceStrings.suggest)

        type("From the tab.")
        tap(open)

        assertEquals("[Guide] RB-T5", mails.single().second)
    }
}
