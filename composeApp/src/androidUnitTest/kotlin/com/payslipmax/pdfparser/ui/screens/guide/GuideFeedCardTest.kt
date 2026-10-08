package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import com.payslipmax.pdfparser.testing.FakeCrashReporter
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

/**
 * Phase E3: case tile, then a feed with breadcrumb and facet chips, then the full card (the approved preview).
 * Synthetic bundle: td-da has 8 cards over four facets (chips), ltc-home has 2 cards plus RB-T1 as an
 * "also relevant here" link (no chips), RB-T1 is the personal card, RB-T3 has no cite.
 *
 * A tall screen keeps every feed row and card section composed, so "does not exist" means left out, never just
 * scrolled off (a lazy list does not compose what is off screen). Scroll restore is tested in `GuideTabTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideFeedCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val viewModel = GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), dispatcher)
    private val searchViewModel = GuideSearchViewModel(viewModel, dispatcher)

    private fun show(navState: GuideNavState = GuideNavState()): GuideNavState {
        composeRule.setContent { GuideTab(navState = navState, access = UnlockedGuideAccess, viewModel = viewModel, searchViewModel = searchViewModel) }
        settle()
        return navState
    }

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun title(id: String) = "Synthetic card $id?"

    private fun openBigCase(): GuideNavState {
        val nav = show()
        tap("Travel")
        tap("Daily allowance on duty")
        return nav
    }

    private fun facetChip(
        label: String,
        count: Int,
    ) = composeRule.onNodeWithContentDescription(GuideStrings.facetChipDescription(label, count))

    @Test
    fun aLongFeedOffersFacetChipsWithCountsAndAllSelected() {
        openBigCase()

        facetChip(GuideStrings.facetAllLabel, 8).assertIsDisplayed().assertIsSelected()
        facetChip("How much", 3).assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText(title("RB-T1")).assertIsDisplayed()
    }

    @Test
    fun aChipFiltersTheFeedInOrderAndIsKeptOnTheStack() {
        val nav = openBigCase()

        facetChip("Who qualifies", 2).performClick()
        settle()

        assertEquals(GuideDestination.Case(SyntheticGuideBundle.BIG_CASE, facet = "Q"), nav.current)
        facetChip("Who qualifies", 2).assertIsSelected()
        composeRule.onNodeWithText(title("RB-T4")).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T5")).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T1")).assertDoesNotExist()
    }

    @Test
    fun aShortFeedShowsItsCountAndNamesTheHomeOfAnAlsoRelevantCard() {
        show()
        tap("Travel")
        tap("Home town LTC")

        composeRule.onNodeWithText(GuideStrings.cardCount(3)).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.facetAllLabel, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.alsoRelevant("Daily allowance on duty")).assertIsDisplayed()
    }

    @Test
    fun anAlsoRelevantCardOpensAndItsBreadcrumbLeadsBackToThatFeed() {
        val nav = show()
        tap("Travel")
        tap("Home town LTC")
        tap(title("RB-T1"))

        composeRule.onNodeWithContentDescription(GuideStrings.breadcrumbDescription("Home town LTC")).performClick()
        settle()

        assertEquals(GuideDestination.Case("ltc-home"), nav.current)
    }

    @Test
    fun theBreadcrumbGoesUpToTheAreaAndToGuideHome() {
        val nav = openBigCase()
        tap(title("RB-T4"))

        composeRule.onNodeWithContentDescription(GuideStrings.breadcrumbDescription("Travel")).performClick()
        settle()
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), nav.stack)

        tap("Daily allowance on duty")
        composeRule.onNodeWithContentDescription(GuideStrings.breadcrumbDescription(GuideStrings.breadcrumbHome)).performClick()
        settle()
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), nav.stack)
    }

    @Test
    fun aCardShowsItsSectionsWithTheCiteInMonospaceAndDetailsCollapsed() {
        openBigCase()
        tap(title(SyntheticGuideBundle.AMENDED_CARD))

        composeRule.onNodeWithText("A one-line answer for RB-T4.").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
        composeRule.onNodeWithText("A short key point for RB-T4").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionAttach).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionWatchOut).assertDoesNotExist() // no watch bullets on this card
        composeRule.onNodeWithText(GuideStrings.sectionAuthority).assertIsDisplayed()
        assertEquals(FontFamily.Monospace, fontFamilyOf("Rule 114 TR"), "the cite reads like a rule reference")
        composeRule.onNodeWithText(GuideStrings.cardDisclaimer).assertIsDisplayed()

        composeRule.onNodeWithText("Longer details for RB-T4.").assertDoesNotExist()
        tap(GuideStrings.details)
        composeRule.onNodeWithText("Longer details for RB-T4.").assertIsDisplayed()
    }

    @Test
    fun aPersonalCardShowsItsBulletsAndNoRawPlaceholder() {
        openBigCase()
        tap(title(SyntheticGuideBundle.PERSONAL_CARD))

        // The card has no template placeholders (the validator rejects them); its figure comes from the bundle's figures.
        composeRule.onAllNodes(hasText("{", substring = true)).assertCountEquals(0)
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionAttach).assertIsDisplayed()
    }

    @Test
    fun aCardWithNoCiteHasNoAuthoritySection() {
        openBigCase()
        tap(title(SyntheticGuideBundle.NO_CITE_CARD))

        composeRule.onNodeWithText(GuideStrings.sectionAuthority).assertDoesNotExist()
    }

    private fun fontFamilyOf(text: String): FontFamily? {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single().layoutInput.style.fontFamily
    }
}
