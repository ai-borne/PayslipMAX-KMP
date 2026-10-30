package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: Part II levels live in the Guide's own stack, so back, the tab re-tap and a restore after process death must treat them like any
 * other level, and a restore must never rebuild a place the bundle (or the flag) no longer offers. Only ids are saved: a ribbon id or a
 * manual code, never a query or a name. The levels are Home, a ribbon and a casualty: a case is a heading, not a level.
 */
class GuideNavPart2Test {
    private val bundle = SyntheticGuidePart2.bundle()
    private val without = SyntheticGuideBundle.withPart2().copy(part2 = null)

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    private val fullStack = listOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Code("FOURTH11"))

    @Test
    fun everyPartTwoLevelRoundTripsAsPlainStringsWithIdsOnly() {
        val saved = save(GuideNavState(fullStack)) as List<*>
        assertTrue(saved.all { it is String })
        assertEquals(listOf("part2|home", "part2|ribbon|leave", "part2|code|FOURTH11"), saved)
        assertEquals(listOf(GuideDestination.Home) + fullStack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aPartTwoLevelAmongOtherGuideLevelsKeepsItsPlace() {
        val stack = listOf(GuideDestination.Area("travel"), Part2Destination.Home, GuideDestination.Card("RB-T9"))
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(save(GuideNavState(stack)) as List<*>)!!.stack)
    }

    @Test
    fun theSavedStackNeverHoldsTheQueryOrAnyNameEvenWhileTheSearchBoxHoldsOne() =
        runTest {
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), FakeCrashReporter(), StandardTestDispatcher(testScheduler))
            val model = Part2LookupViewModel(guide)
            val nav = GuideNavState(fullStack).also { it.part2Lookup = model }
            model.onQueryChange("secretquery")
            val saved = (save(nav) as List<*>).joinToString("|")
            assertFalse("secretquery" in saved, "the query is memory only")
            assertFalse("Granted" in saved, "no manual name is saved: ids only")
        }

    @Test
    fun aMalformedPartTwoEntryIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2", "part2|", "part2|nope", "part2|home|extra", "part2|ribbon", "part2|ribbon|", "part2|case|leave_one", "part2|ribbon|a|b", "part2|code|", "part2|code|A|B")) {
            val restored = GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!
            assertEquals(listOf(GuideDestination.Home), restored.stack, "'$bad' must not restore, nor the card above it")
        }
    }

    @Test
    fun thePartTwoLevelsAreKnownOnlyToABundleThatShipsThemAndOnlyForRealIds() {
        for (level in fullStack) assertTrue(bundle.knows(level), "$level")
        assertTrue(bundle.knows(Part2Destination.Code("VARIANT2")), "any code of a variant serial")
        assertTrue(bundle.knows(Part2Destination.Ribbon("records")), "every ribbon is a place in Learn, whether or not its orders can be drafted")
        assertTrue(bundle.knows(Part2Destination.Code("STOPCODE")), "search reaches every casualty")
        assertFalse(bundle.knows(Part2Destination.Ribbon("nope")))
        assertFalse(bundle.knows(Part2Destination.Code("NOSUCHCODE")))
        assertFalse(bundle.knows(Part2Destination.Code("firstcode")), "a code is the manual's capitals")
        for (level in fullStack) assertFalse(without.knows(level), "a bundle with no Part II section knows no Part II place: $level")
    }

    @Test
    fun aRestoredStackIsCutAtTheFirstPartTwoLevelTheBundleLacks() {
        val nav = GuideNavState(listOf(GuideDestination.Area("travel"), Part2Destination.Home, Part2Destination.Ribbon("nope"), Part2Destination.Code("FIRSTCODE")))
        nav.retainKnown(bundle)
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel"), Part2Destination.Home), nav.stack, "an unknown id drops its level and everything above")
        val code = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Code("NOSUCHCODE")))
        code.retainKnown(bundle)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Ribbon("leave")), code.stack)
        val old = GuideNavState(fullStack)
        old.retainKnown(without)
        assertEquals(listOf(GuideDestination.Home), old.stack, "an old bundle cuts the whole Part II branch")
    }

    @Test
    fun backPopsExactlyOneLevelAndTheTabReTapGoesHome() {
        val nav = GuideNavState(fullStack)
        assertTrue(nav.pop())
        assertEquals(Part2Destination.Ribbon("leave"), nav.current)
        assertTrue(nav.pop())
        assertEquals(Part2Destination.Home, nav.current)
        nav.push(Part2Destination.Code("FIRSTCODE"))
        nav.popToHome()
        assertEquals(listOf(GuideDestination.Home), nav.stack)
    }

    @Test
    fun eachLevelRemembersItsOwnScrollAcrossARestore() {
        val nav = GuideNavState(fullStack)
        nav.saveScroll(GuideScroll(7, 12), level = 1)
        val restored = GuideNavStateSaver.restore(save(nav) as List<*>)!!
        assertEquals(GuideScroll(7, 12), restored.savedScrolls[1])
    }

    @Test
    fun aLockDropsTheSearchTextButKeepsTheOfficersPlace() =
        runTest {
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), FakeCrashReporter(), StandardTestDispatcher(testScheduler))
            val model = Part2LookupViewModel(guide)
            val nav = GuideNavState(fullStack).also { it.part2Lookup = model }
            model.onQueryChange("variant")
            nav.discardTransientForms()
            assertEquals("", model.query.value, "typed text is gone when the app locks")
            assertEquals(listOf(GuideDestination.Home) + fullStack, nav.stack, "the stack stays, so unlocking returns to the same level")
        }

    @Test
    fun aLockWithNoLearnModelBoundIsHarmless() {
        GuideNavState(fullStack).discardTransientForms()
    }
}
