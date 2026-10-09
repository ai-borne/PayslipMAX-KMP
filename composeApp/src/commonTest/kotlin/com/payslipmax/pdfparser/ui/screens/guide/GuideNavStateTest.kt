package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Guide keeps its own stack inside its tab (owner decision 2026-10-07): Home is always at the bottom. */
class GuideNavStateTest {
    private val bundle = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle

    @Test
    fun startsAtHomeWithNothingToPop() {
        val state = GuideNavState()
        assertEquals(GuideDestination.Home, state.current)
        assertFalse(state.canPop)
        assertFalse(state.pop(), "pop at Home must be a no-op so Android back leaves the tab")
    }

    @Test
    fun pushAndPopWalkTheStackOneLevelAtATime() {
        val state = GuideNavState()
        state.push(GuideDestination.Area("travel"))
        state.push(GuideDestination.Case("td-da"))
        assertEquals(GuideDestination.Case("td-da"), state.current)

        assertTrue(state.pop())
        assertEquals(GuideDestination.Area("travel"), state.current)
        assertTrue(state.pop())
        assertEquals(GuideDestination.Home, state.current)
    }

    @Test
    fun openingTheCardJustLeftReturnsToItInsteadOfStackingAnotherLevel() {
        // Old rule -> See current rule -> Earlier rule: the second tap is a way back, so Back never has to walk a loop.
        val state = GuideNavState()
        state.push(GuideDestination.Card("OLD"))
        state.openCardOrReturn("NEW")
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Card("OLD"), GuideDestination.Card("NEW")), state.stack)

        state.openCardOrReturn("OLD")

        assertEquals(listOf(GuideDestination.Home, GuideDestination.Card("OLD")), state.stack)
    }

    @Test
    fun openingACardThatIsNotJustBelowStillStacksIt() {
        val state = GuideNavState()
        state.push(GuideDestination.Card("A"))
        state.push(GuideDestination.Card("B"))
        state.push(GuideDestination.Card("C"))

        state.openCardOrReturn("A")

        assertEquals(GuideDestination.Card("A"), state.current)
        assertEquals(5, state.stack.size, "A is two levels down, not the level just left, so it stacks and Back walks C, B, A")
    }

    @Test
    fun popToHomeClearsEveryLevel() {
        // Re-tapping the active Guide tab goes back to Guide Home (owner decision 2026-10-07).
        val state = GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.Case("td-da")))
        state.popToHome()
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), state.stack)
    }

    @Test
    fun pushingHomeNeverStacksASecondHome() {
        val state = GuideNavState(listOf(GuideDestination.Area("travel")))
        state.push(GuideDestination.Home)
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), state.stack)
    }

    @Test
    fun retainKnownCutsTheStackAtTheFirstIdTheBundleDoesNotHave() {
        // Same rule as AppNavStateSaver: never rebuild an order the user did not create, so a valid entry after
        // a stale one is dropped too.
        val state =
            GuideNavState(
                listOf(
                    GuideDestination.Area("travel"),
                    GuideDestination.Case("removed-case"),
                    GuideDestination.Card(SyntheticGuideBundle.PERSONAL_CARD),
                ),
            )
        state.retainKnown(bundle)
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), state.stack)
    }

    @Test
    fun bundleKnowsEachDestinationKindByItsOwnIds() {
        assertTrue(bundle.knows(GuideDestination.Home))
        assertTrue(bundle.knows(GuideDestination.Search))
        assertTrue(bundle.knows(GuideDestination.Area("pay")))
        assertFalse(bundle.knows(GuideDestination.Area("td-da")), "a case id is not an area id")
        assertTrue(bundle.knows(GuideDestination.Case(SyntheticGuideBundle.BIG_CASE, facet = "H")))
        assertFalse(bundle.knows(GuideDestination.Case(SyntheticGuideBundle.BIG_CASE, facet = "Z")), "unknown facet")
        assertTrue(bundle.knows(GuideDestination.Card("RB-P3")))
        assertFalse(bundle.knows(GuideDestination.Card("RB-P4")))
    }

    @Test
    fun selectingAFacetReplacesTheCaseOnTopSoItSurvivesRestore() {
        val state = GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.Case("td-da")))

        state.selectFacet("H")
        assertEquals(GuideDestination.Case("td-da", facet = "H"), state.current)
        assertEquals(3, state.stack.size, "a chip filters the feed; it is not a level back has to undo")

        state.selectFacet(null)
        assertEquals(GuideDestination.Case("td-da"), state.current)
    }

    @Test
    fun selectingAFacetAnywhereButAFeedIsIgnored() {
        val state = GuideNavState(listOf(GuideDestination.Card("RB-T1")))
        state.selectFacet("H")
        assertEquals(GuideDestination.Card("RB-T1"), state.current)
    }

    @Test
    fun theBreadcrumbGoesUpToALevelAlreadyOnTheStack() {
        val case = GuideDestination.Case("td-da", facet = "H")
        val state = GuideNavState(listOf(GuideDestination.Area("travel"), case, GuideDestination.Card("RB-T1")))

        state.upTo(listOf(GuideDestination.Area("travel"), case))
        assertEquals(case, state.current, "the feed comes back with its chosen facet")

        state.upTo(listOf(GuideDestination.Area("travel")))
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), state.stack)

        state.upTo(emptyList())
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), state.stack)
    }

    @Test
    fun theBreadcrumbBuildsThePathUpWhenTheCardWasOpenedFromElsewhere() {
        // A card opened from search (E4) or Pay Audit (E7) has no area or case below it; its breadcrumb still
        // leads to the card's own case, and back from there walks up the same path.
        val state = GuideNavState(listOf(GuideDestination.Search, GuideDestination.Card("RB-T1")))

        state.upTo(listOf(GuideDestination.Area("travel"), GuideDestination.Case("td-da")))

        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel"), GuideDestination.Case("td-da")), state.stack)
    }

    @Test
    fun eachLevelKeepsItsOwnScrollUntilItIsLeft() {
        val state = GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.Case("td-da")))
        state.saveScroll(GuideScroll(5, 30))
        state.push(GuideDestination.Card("RB-T1"))
        assertEquals(GuideScroll.Top, state.currentScroll, "a new level starts at the top")
        state.saveScroll(GuideScroll(2, 0))

        state.pop()
        assertEquals(GuideScroll(5, 30), state.currentScroll, "back returns to the same place in the feed")

        state.push(GuideDestination.Card("RB-T2"))
        assertEquals(GuideScroll.Top, state.currentScroll, "a popped level's scroll is not reused by the next card")
    }

    @Test
    fun aNewFacetStartsTheFeedAtTheTop() {
        val state = GuideNavState(listOf(GuideDestination.Case("td-da")))
        state.saveScroll(GuideScroll(6, 10))

        state.selectFacet("Q")

        assertEquals(GuideScroll.Top, state.currentScroll)
    }

    @Test
    fun goingHomeOrCuttingTheStackForgetsTheScrollAboveIt() {
        val state = GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.Case("removed")))
        state.saveScroll(GuideScroll(4, 0))
        state.retainKnown(bundle)
        state.push(GuideDestination.Case("td-da"))
        assertEquals(GuideScroll.Top, state.currentScroll)

        state.saveScroll(GuideScroll(3, 0))
        state.popToHome()
        state.push(GuideDestination.Area("travel"))
        state.push(GuideDestination.Case("td-da"))
        assertEquals(GuideScroll.Top, state.currentScroll)
    }
}
