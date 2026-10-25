package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the Due list is a Guide level like any other, so back, a restore after process death and a flag-off build must treat it so. It
 * carries no id (it lists what the repository holds), and the shortcut the Dashboard card uses must always land on the same three levels,
 * whatever the Guide stack held before, so Back from Due walks Part II home then Guide Home.
 */
class GuideNavPart2DueTest {
    private val bundle = SyntheticPart2Lookup.bundle()
    private val without = SyntheticGuideBundle.withPart2().copy(part2 = null)

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    @Test
    fun theDueLevelRoundTripsAsOnePlainWord() {
        val saved = save(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Due))) as List<*>
        assertEquals(listOf("part2|home", "part2|due"), saved)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Due), GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aDueEntryWithAnythingAfterItIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2|due|x", "part2|due|")) {
            assertEquals(listOf(GuideDestination.Home), GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!.stack, "'$bad' must not restore, nor the card above it")
        }
    }

    @Test
    fun theDueLevelIsKnownOnlyWithAPartTwoSection() {
        assertTrue(bundle.knows(Part2Destination.Due))
        assertFalse(without.knows(Part2Destination.Due))
    }

    @Test
    fun aRestoredStackWithoutAPartTwoSectionIsCutBackToTheGuideHome() {
        val state = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Due))
        state.retainKnown(without)
        assertEquals(listOf(GuideDestination.Home), state.stack)
    }

    @Test
    fun theDueLevelIsNotADraftOrAnOrderSoNoCutByIdTouchesIt() {
        val state = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Due))
        state.cutDraft("d1")
        state.cutOrder("o1")
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Due), state.stack)
        assertNull((Part2Destination.Due as Part2Destination).draftId)
        assertNull((Part2Destination.Due as Part2Destination).orderId)
    }

    @Test
    fun openingDueFromAnywhereLandsOnGuideHomePartTwoHomeDue() {
        val deep = GuideNavState(listOf(GuideDestination.Search, GuideDestination.Card("RB-T1")))
        deep.openPart2Due()
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Due), deep.stack)

        val already = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Due))
        already.openPart2Due()
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Due), already.stack)
    }

    @Test
    fun backFromDueWalksPartTwoHomeThenGuideHome() {
        val state = GuideNavState()
        state.openPart2Due()
        assertTrue(state.pop())
        assertEquals(Part2Destination.Home, state.current)
        assertTrue(state.pop())
        assertEquals(GuideDestination.Home, state.current)
    }
}
