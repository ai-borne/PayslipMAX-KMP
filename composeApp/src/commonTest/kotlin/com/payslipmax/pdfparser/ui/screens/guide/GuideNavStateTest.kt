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
}
