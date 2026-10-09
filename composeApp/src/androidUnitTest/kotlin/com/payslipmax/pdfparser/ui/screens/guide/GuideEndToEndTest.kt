package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.screens.PayAuditMonthFindings
import com.payslipmax.pdfparser.ui.screens.payAuditFindingsItems
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E9 end to end, on the REAL shipped bundle (so a renamed card, a moved case or a broken link fails here, not on a phone):
 * (1) Guide tab: tile, case, card, Pin, back to Home, the Pinned row reopens the same card, and the pin survives a rebuilt
 * model on the same storage; (2) Pay Audit: "Read the rule" opens the card the owner mapped, and Back returns to the same
 * finding. A tall screen keeps every section composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideEndToEndTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val storage = FakeGuidePinsStorage()
    private val repository =
        FakeGuideRepository(GuideBundleParser.parse(runBlocking { GuideBundleContract.readShippedBundleText() }))

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun guideModel() = GuideViewModel(repository, crash, dispatcher, pins = GuidePinsModel(storage))

    @Test
    fun tileToCaseToCardToPinToHomeAndTheSameCardReopens() {
        val guide = guideModel()
        val nav = GuideNavState()
        composeRule.setContent {
            GuideTab(
                navState = nav,
                access = GuideAccess(isUnlocked = true, onUnlock = {}),
                viewModel = guide,
                searchViewModel = GuideSearchViewModel(guide, dispatcher),
                platform = GuidePlatform(copy = {}, share = { _, _ -> }),
            )
        }
        settle()

        tap("My pay")
        tap("7th CPC pay structure")
        tap(MSP_TITLE)
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
        tap(GuideStrings.pin)
        assertEquals(MSP_ID, storage.stored, "a pin is the plain card id, nothing else")

        composeRule.runOnUiThread { nav.popToHome() }
        settle()
        composeRule.onNodeWithText(GuideStrings.pinnedSection).assertIsDisplayed()
        tap(MSP_TITLE)
        assertEquals(GuideDestination.Card(MSP_ID), nav.current)
        composeRule.onNodeWithText(GuideStrings.unpin).assertIsDisplayed()

        assertEquals(listOf(MSP_ID), GuidePinsModel(storage).pins.value.newestFirst, "a rebuilt model (process death) still holds the pin")
        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty(), "the walk tells telemetry nothing")
    }

    @Test
    fun theRealBundleShowsAWhatsNewRowExactlyWhenItCarriesAChangeEntry() {
        val guide = guideModel()
        composeRule.setContent {
            GuideTab(
                navState = GuideNavState(),
                access = GuideAccess(isUnlocked = true, onUnlock = {}),
                viewModel = guide,
                searchViewModel = GuideSearchViewModel(guide, dispatcher),
                platform = GuidePlatform(copy = {}, share = { _, _ -> }),
            )
        }
        settle()

        val hasEntry = (repository.result as GuideLoadResult.Loaded).bundle.changes.isNotEmpty()
        composeRule.onAllNodesWithText("What's new", substring = true).assertCountEquals(if (hasEntry) 1 else 0)
        composeRule.onNodeWithText(GuideStrings.homeTitle).assertIsDisplayed()
    }

    @Test
    fun aRuleChangeWalkFromHomeToTheNewRuleToTheOldRuleAndBack() {
        val repo = FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideRuleChange.bundle()))
        val guide = GuideViewModel(repo, crash, dispatcher, pins = GuidePinsModel(storage))
        val nav = GuideNavState()
        composeRule.setContent {
            GuideTab(
                navState = nav,
                access = GuideAccess(isUnlocked = true, onUnlock = {}),
                viewModel = guide,
                searchViewModel = GuideSearchViewModel(guide, dispatcher),
                platform = GuidePlatform(copy = {}, share = { _, _ -> }),
            )
        }
        settle()

        tap(GuideMaintenanceStrings.whatsNewRow(SyntheticGuideRuleChange.LATEST))
        composeRule.onAllNodesWithText("Synthetic card RB-T11?")[0].performClick()
        settle()
        tap(GuideMaintenanceStrings.earlierRule(SyntheticGuideRuleChange.EFFECTIVE))
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipReplacedOn(SyntheticGuideRuleChange.EFFECTIVE)).assertIsDisplayed()
        tap(GuideMaintenanceStrings.seeCurrentRule)
        // See current rule from the old card returns to the new card it came from, so the stack does not grow in a loop.
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Changes, GuideDestination.Card("RB-T11")), nav.stack)

        repeat(2) { composeRule.runOnUiThread { nav.pop() } }
        settle()
        assertEquals(GuideDestination.Home, nav.current)
        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty(), "reading rule history tells telemetry nothing")
    }

    /**
     * M5 on the real bundle: a pinned row on Home opens the card, Suggest a correction takes the user's words, and Open email
     * hands the mail seam exactly the allow-listed message with the card's real revision and the bundle's real date; nothing
     * else of the card (its key points, its cite) and nothing to telemetry. The screen is the Robolectric default on purpose:
     * under a `w360dp-...` screen the dialog's text layout never settles and the test JVM runs out of memory.
     */
    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h470dp")
    fun aSuggestionFromAPinnedRealCardOpensTheMailAppWithOnlyTheAllowListedFields() {
        storage.stored = MSP_ID
        val mails = mutableListOf<Triple<String, String, String>>()
        val guide = GuideViewModel(repository, crash, dispatcher, pins = GuidePinsModel(storage), appVersion = { "1.3.0" })
        composeRule.setContent {
            GuideTab(
                navState = GuideNavState(),
                access = GuideAccess(isUnlocked = true, onUnlock = {}),
                viewModel = guide,
                searchViewModel = GuideSearchViewModel(guide, dispatcher),
                platform = GuidePlatform(copy = {}, share = { _, _ -> }, email = { to, subject, body -> mails += Triple(to, subject, body) }),
            )
        }
        settle()

        tap(MSP_TITLE)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(GuideMaintenanceStrings.suggest))
        tap(GuideMaintenanceStrings.suggest)
        assertTrue(mails.isEmpty(), "opening the dialog opens no mail app")
        composeRule.onNode(hasSetTextAction()).performTextInput("The rate is now Rs 1,500 & the order is amended.")
        tap(GuideMaintenanceStrings.suggestOpenEmail)

        val bundle = (repository.result as GuideLoadResult.Loaded).bundle
        val card = bundle.cards.first { it.id == MSP_ID }
        assertTrue(card.rev.isNotBlank() && bundle.generated.isNotBlank(), "the shipped bundle carries both, so the check below is not vacuous")
        val (to, subject, body) = mails.single()
        assertEquals(AppStringsSupport.supportEmail, to)
        assertEquals("[Guide] $MSP_ID", subject)
        assertEquals(
            "Guide correction suggestion\n\nCard: $MSP_ID\nTitle: $MSP_TITLE\nGuide data: ${bundle.generated}\n" +
                "Card revision: ${card.rev}\nApp version: 1.3.0\n\nSuggestion:\nThe rate is now Rs 1,500 & the order is amended.",
            body,
        )
        assertTrue(card.key.none { body.contains(it) } && !body.contains(card.cite), "no card text beyond the title")
        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty(), "suggesting tells telemetry nothing")
    }

    private val arrears = Anomaly("ARREARS_AUDIT", "arrearsDa", 9870.0, "04/2026", "Verified: matches exactly.", expected = 9870.0, actual = 9870.0)

    private fun showFindingThenCard(unlocked: Boolean): IntArray {
        val guide = guideModel()
        val counts = intArrayOf(0)
        composeRule.setContent {
            var onCard by remember { mutableStateOf(false) }
            if (onCard) {
                GuideCardHost(GuideAccess(unlocked, onUnlock = {}), onBack = { onCard = false }, viewModel = guide, platform = GuidePlatform(copy = {}, share = { _, _ -> }))
            } else {
                LazyColumn {
                    payAuditFindingsItems(
                        PayAuditMonthFindings(verified = listOf(arrears)),
                        0,
                        {},
                        {},
                        onOpenGuideCard = { id -> openGuideCardFromFinding(id, unlocked, guide, onLocked = { counts[0]++ }, onOpened = { onCard = true }) },
                    )
                }
            }
        }
        settle()
        return counts
    }

    @Test
    fun payAuditLinkOpensTheArrearsCardAndBackReturnsToTheFinding() {
        showFindingThenCard(unlocked = true)

        tap(GuideStrings.payAuditSeeRule)
        composeRule.onNodeWithText(DA_ARREARS_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(AppStrings.btnBack).performClick()
        settle()
        composeRule.onNodeWithText(GuideStrings.payAuditSeeRule).assertIsDisplayed()
        composeRule.onNodeWithText(DA_ARREARS_TITLE).assertDoesNotExist()
    }

    @Test
    fun aLockedUsersPayAuditLinkAsksForTheUpgradeAndNeverOpensACard() {
        val upgradeAsks = showFindingThenCard(unlocked = false)

        tap(GuideStrings.payAuditSeeRule)

        assertEquals(1, upgradeAsks[0])
        composeRule.onNodeWithText(DA_ARREARS_TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.payAuditSeeRule).assertIsDisplayed()
    }

    private companion object {
        const val MSP_ID = "RB-C13-05"
        const val MSP_TITLE = "Military Service Pay: who gets it and how much?"
        const val DA_ARREARS_TITLE = "DA arrears: what are they and what do I check?"
    }
}
