package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Guide stack survives process death as plain ids (plan: "Navigation and state management"). Restore runs
 * before the bundle has loaded, so it only rejects entries it cannot read; ids are checked once loading ends.
 */
class GuideNavStateSaverTest {
    private val bundle = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    @Test
    fun everyDestinationKindRoundTrips() {
        val stack =
            listOf(
                GuideDestination.Area("travel"),
                GuideDestination.Case("td-da", facet = "H"),
                GuideDestination.Card("RB-T1"),
                GuideDestination.Search,
            )
        val restored = GuideNavStateSaver.restore(save(GuideNavState(stack)))!!
        assertEquals(listOf(GuideDestination.Home) + stack, restored.stack)
    }

    @Test
    fun aCaseWithoutAFacetRestoresWithoutOne() {
        val restored = GuideNavStateSaver.restore(save(GuideNavState(listOf(GuideDestination.Case("td-da")))))!!
        assertEquals(GuideDestination.Case("td-da", facet = null), restored.current)
    }

    @Test
    fun savedFormHoldsOnlyPlainStrings() {
        // Bundle-safe on Android, and nothing but ids: no card text or search query is ever written out.
        val saved = save(GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.Search))) as List<*>
        assertEquals(listOf("area|travel", "search"), saved)
    }

    @Test
    fun corruptEntryCutsItAndEverythingAfterIt() {
        val restored = GuideNavStateSaver.restore(listOf("area|travel", "bogus|x", "card|RB-T1"))!!
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), restored.stack)

        val notAString = GuideNavStateSaver.restore(listOf("area|travel", 42, "card|RB-T1"))!!
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), notAString.stack)

        val missingId = GuideNavStateSaver.restore(listOf("card|", "area|travel"))!!
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), missingId.stack)
    }

    @Test
    fun restoreBeforeTheBundleLoadsIsValidatedWhenLoadingEnds() {
        // A bundle update removed a card the user had open: the restored stack keeps it until the bundle is
        // known, then cuts there rather than crashing or showing a blank screen.
        val restored = GuideNavStateSaver.restore(listOf("area|travel", "case|td-da", "card|RB-GONE"))!!
        assertEquals(GuideDestination.Card("RB-GONE"), restored.current)

        restored.retainKnown(bundle)

        assertEquals(GuideDestination.Case("td-da"), restored.current)
    }

    @Test
    fun theFacetAndScrollOfEveryLevelSurviveProcessDeath() {
        val state = GuideNavState(listOf(GuideDestination.Area("travel")))
        state.saveScroll(GuideScroll(1, 12))
        state.push(GuideDestination.Case("td-da"))
        state.selectFacet("H")
        state.saveScroll(GuideScroll(4, 25))
        state.push(GuideDestination.Card("RB-T1"))
        state.pop()

        val restored = GuideNavStateSaver.restore(save(state))!!

        assertEquals(GuideDestination.Case("td-da", facet = "H"), restored.current)
        assertEquals(GuideScroll(4, 25), restored.currentScroll)
        restored.pop()
        assertEquals(GuideScroll(1, 12), restored.currentScroll)
    }

    @Test
    fun scrollIsSavedAsPlainNumbersAndOnlyWhenNotAtTheTop() {
        val state = GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.Case("td-da")))
        state.saveScroll(GuideScroll(3, 40))
        assertEquals(listOf("area|travel", "case|td-da", "scroll|2|3|40"), save(state))
    }

    @Test
    fun aCorruptOrOrphanScrollEntryIsIgnoredWithoutLosingTheStack() {
        val restored =
            GuideNavStateSaver.restore(listOf("area|travel", "scroll|1|x|0", "scroll|9|2|0", "scroll|1|-1|0", "scroll|1|2"))!!
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), restored.stack)
        assertEquals(GuideScroll.Top, restored.currentScroll)
    }

    @Test
    fun theWhatsNewListRoundTripsAsABareWordAndSavesNoText() {
        val stack = listOf(GuideDestination.Changes, GuideDestination.Card("RB-T11"))

        val saved = save(GuideNavState(stack)) as List<*>
        val restored = GuideNavStateSaver.restore(saved)!!

        assertEquals(listOf("changes", "card|RB-T11"), saved, "a level name and an id, never change text")
        assertEquals(listOf(GuideDestination.Home) + stack, restored.stack)
    }

    @Test
    fun aChangesEntryWithExtraPartsIsCutLikeAnyUnreadableEntry() {
        val restored = GuideNavStateSaver.restore(listOf("changes|junk", "card|RB-T1"))!!

        assertEquals(listOf<GuideDestination>(GuideDestination.Home), restored.stack)
    }

    @Test
    fun theWhatsNewListIsKnownOnlyToABundleThatHasChanges() {
        val withChanges = SyntheticGuideRuleChange.bundle()

        assertEquals(true, withChanges.knows(GuideDestination.Changes))
        assertEquals(false, bundle.knows(GuideDestination.Changes))
        val state = GuideNavState(listOf(GuideDestination.Changes, GuideDestination.Card("RB-T1")))
        state.retainKnown(bundle)
        assertEquals(listOf<GuideDestination>(GuideDestination.Home), state.stack, "an update that dropped the log cuts the restored list and what was above it")
    }
}
