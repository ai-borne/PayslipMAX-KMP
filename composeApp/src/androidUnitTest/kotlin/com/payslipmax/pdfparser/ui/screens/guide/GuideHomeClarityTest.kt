package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Entry
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimEntry
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: users could not tell what the three parts of Guide Home are for (the change log, the rule topics, their own Part II file),
 * because all three looked alike and said nothing about their purpose. Each group now names itself, and the Part II group says it is the
 * user's own, private file. The last test guards the other half of the intent: the clearer Home must still fit one phone screen, so the
 * personal group is not pushed below the fold (the failure that made us compress the layout).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h2000dp")
class GuideHomeClarityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val areas = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }.toReady().areas
    private val whatsNewRow = GuideMaintenanceStrings.whatsNewRow("2026-10-30")

    private fun showHome(
        preClaim: PreClaimEntry = PreClaimEntry.LOCKED,
        part2: Part2Entry = Part2Entry.OPEN,
        part2Status: HubStatus = HubStatus.Unknown,
        isNew: Boolean = false,
        onDismiss: () -> Unit = {},
    ) = composeRule.setContent {
        GuideHomeScreen(
            areas, onOpenArea = {}, onOpenSearch = {}, whatsNewDate = "2026-10-30", whatsNewIsNew = isNew, whatsNewCount = 3, onDismissWhatsNew = onDismiss,
            preClaim = preClaim, part2 = part2, part2Status = part2Status,
        )
    }

    private fun top(text: String) = composeRule.onAllNodesWithText(text)[0].fetchSemanticsNode().boundsInRoot.top

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun theRuleTopicsNameThemselvesAsReferenceBetweenTheChangeLogAndTheFirstTopic() {
        showHome()
        composeRule.onNodeWithText(GuideStrings.homeOverview).assert(isHeading).assertIsDisplayed()
        val firstArea = areas.first().let { GuideStrings.areaTileLabel(it.id) ?: it.title }
        assertTrue(top(whatsNewRow) < top(GuideStrings.homeOverview), "the change log row is above the topics label")
        assertTrue(top(GuideStrings.homeOverview) < top(firstArea), "the topics label is above the first topic")
    }

    @Test
    fun theScreenSubtitleSaysWhatTheGuideIsForNotJustWhatItContains() {
        showHome()
        composeRule.onNodeWithText(GuideStrings.homeSection).assertIsDisplayed()
        assertTrue(GuideStrings.homeSection.contains("claim"), "the subtitle says the guide helps with claiming")
    }

    @Test
    fun thePartTwoGroupIsLabelledAsTheUsersOwnPrivateFileAndItsTileSaysWhatIsInside() {
        showHome()
        composeRule.onNodeWithText(LearnStrings.sectionHeading).assert(isHeading).assertIsDisplayed()
        composeRule.onNodeWithText(LearnStrings.sectionSubtext).assertIsDisplayed()
        composeRule.onNodeWithText(LearnStrings.hubTileSubtitle).assertIsDisplayed()
        // The old subtitle only repeated the title; the new one must say what is inside the hub: Learn, and the Orders Bank.
        assertTrue(LearnStrings.hubTileSubtitle.contains("Learn", ignoreCase = true), "the tile subtitle says there is something to learn")
        assertTrue(LearnStrings.hubTileSubtitle.contains("keep", ignoreCase = true), "the tile subtitle says orders are kept")
        assertFalse(LearnStrings.hubTileSubtitle.contains("Manage", ignoreCase = true), "the old, empty 'Manage' wording is gone")
        assertTrue(top(LearnStrings.hubTitle) > top(LearnStrings.sectionHeading), "the tile is under its label")
    }

    @Test
    fun theChangeLogRowIsOneTappableRowWithItsDate() {
        showHome()
        composeRule.onNodeWithText(whatsNewRow).assertHasClickAction().assertIsDisplayed()
    }

    @Test
    fun withTheFlagsOnHomeIsNoTallerThanBeforeTheClarityChangeSoItStillFitsAPhoneScreen() {
        // WHY: before this change Home (change-log row, 9 topics + TA/DA, Part II tile) ended at 1022 dp in this harness, which on the
        // owner's tall Pixel fitted above the tab bar with about 39 dp to spare. Labels and a clearer Part II tile must not push the
        // user's own tile below the fold. This harness draws text taller than a device, so the number is a baseline, not a screen size.
        showHome()
        val bottom = composeRule.onAllNodesWithText(LearnStrings.hubTitle)[0].fetchSemanticsNode().boundsInRoot.bottom / composeRule.density.density
        assertTrue(bottom <= BASELINE_PART2_TILE_BOTTOM_DP, "Part II tile now ends at $bottom dp, below the $BASELINE_PART2_TILE_BOTTOM_DP dp it ended at before")
    }

    @Test
    fun aNewEntryIsABannerWithAChipACountAndADismissButton() {
        var dismissed = 0
        showHome(isNew = true, onDismiss = { dismissed++ })

        composeRule.onNodeWithText(GuideMaintenanceStrings.whatsNewChip).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.whatsNewSummary(3)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(GuideMaintenanceStrings.whatsNewDismiss).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun aSeenEntryIsTheQuietRowWithNoChipNoCountAndNoDismissButButStillOpensTheChangeLog() {
        showHome(isNew = false)

        composeRule.onNodeWithText(whatsNewRow).assertHasClickAction().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(GuideMaintenanceStrings.whatsNewChip).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText(GuideMaintenanceStrings.whatsNewSummary(3)).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription(GuideMaintenanceStrings.whatsNewDismiss).fetchSemanticsNodes().size)
    }

    @Test
    fun theNewBannerAlsoStaysWithinTheHeightBaseline() {
        showHome(isNew = true, part2Status = HubStatus.Filing(total = 12, attached = 8, notAttached = 4))
        val bottom = composeRule.onAllNodesWithText(LearnStrings.hubTitle)[0].fetchSemanticsNode().boundsInRoot.bottom / composeRule.density.density
        assertTrue(bottom <= BASELINE_PART2_TILE_BOTTOM_DP, "with the banner the Part II tile ends at $bottom dp, below the $BASELINE_PART2_TILE_BOTTOM_DP dp baseline")
    }

    @Test
    fun aFilingStatusLineOnThePartTwoTileDoesNotPushItBelowTheSameBaseline() {
        // WHY: the status line is longer than the plain one and may wrap on a narrow phone; it must still fit the budget above.
        showHome(part2Status = HubStatus.Filing(total = 12, attached = 8, notAttached = 4))
        val bottom = composeRule.onAllNodesWithText(LearnStrings.hubTitle)[0].fetchSemanticsNode().boundsInRoot.bottom / composeRule.density.density
        assertTrue(bottom <= BASELINE_PART2_TILE_BOTTOM_DP, "Part II tile with a status line ends at $bottom dp, below the $BASELINE_PART2_TILE_BOTTOM_DP dp baseline")
    }

    private companion object {
        const val BASELINE_PART2_TILE_BOTTOM_DP = 1022f
    }
}
