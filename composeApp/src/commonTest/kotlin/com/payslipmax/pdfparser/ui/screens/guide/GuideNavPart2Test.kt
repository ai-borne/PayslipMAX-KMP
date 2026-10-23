package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: Part II levels live in the Guide's own stack, so back, the tab re-tap and a restore after process death must treat them like any
 * other level, and a restore must never rebuild a place the bundle (or the flag) no longer offers. Only ids are saved: a ribbon id, a
 * case id or a manual code, never a query or a name.
 */
class GuideNavPart2Test {
    private val bundle = SyntheticPart2Lookup.bundle()
    private val without = SyntheticGuideBundle.withPart2().copy(part2 = null)

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    private val fullStack =
        listOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Case("leave_two"), Part2Destination.Code("REJOINAL"))

    @Test
    fun everyPartTwoLevelRoundTripsAsPlainStringsWithIdsOnly() {
        val saved = save(GuideNavState(fullStack)) as List<*>
        assertTrue(saved.all { it is String })
        assertEquals(listOf("part2|home", "part2|ribbon|leave", "part2|case|leave_two", "part2|code|REJOINAL"), saved)
        assertEquals(listOf(GuideDestination.Home) + fullStack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aPartTwoLevelAmongOtherGuideLevelsKeepsItsPlace() {
        val stack = listOf(GuideDestination.Area("travel"), Part2Destination.Home, GuideDestination.Card("RB-T9"))
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(save(GuideNavState(stack)) as List<*>)!!.stack)
    }

    @Test
    fun aMalformedPartTwoEntryIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2", "part2|", "part2|nope", "part2|home|extra", "part2|ribbon", "part2|ribbon|", "part2|case|a|b", "part2|code|", "part2|code|A|B")) {
            val restored = GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!
            assertEquals(listOf(GuideDestination.Home), restored.stack, "'$bad' must not restore, nor the card above it")
        }
    }

    @Test
    fun thePartTwoLevelsAreKnownOnlyToABundleThatShipsThemAndOnlyForBuiltPlaces() {
        for (level in fullStack) assertTrue(bundle.knows(level), "$level")
        assertTrue(bundle.knows(Part2Destination.Code("VARIANT2")), "any code of a variant serial")
        assertTrue(bundle.knows(Part2Destination.Code(SyntheticPart2Lookup.LATER_CODE)), "search reaches an unbuilt ribbon's codes")
        assertFalse(bundle.knows(Part2Destination.Ribbon("career")), "an unbuilt ribbon")
        assertFalse(bundle.knows(Part2Destination.Case("career_one")))
        assertFalse(bundle.knows(Part2Destination.Ribbon("nope")))
        assertFalse(bundle.knows(Part2Destination.Code("NOSUCHCODE")))
        for (level in fullStack) assertFalse(without.knows(level), "a bundle with no Part II section knows no Part II place: $level")
    }

    @Test
    fun aRestoredStackIsCutAtTheFirstPartTwoLevelTheBundleLacks() {
        val nav = GuideNavState(listOf(GuideDestination.Area("travel"), Part2Destination.Home, Part2Destination.Ribbon("career")))
        nav.retainKnown(bundle)
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel"), Part2Destination.Home), nav.stack)
        val old = GuideNavState(fullStack)
        old.retainKnown(without)
        assertEquals(listOf(GuideDestination.Home), old.stack, "an old bundle cuts the whole Part II branch")
    }

    @Test
    fun backPopsOneLevelAndTheTabReTapGoesHome() {
        val nav = GuideNavState(fullStack)
        assertTrue(nav.pop())
        assertEquals(Part2Destination.Case("leave_two"), nav.current)
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
}
