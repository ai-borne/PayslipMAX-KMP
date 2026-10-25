package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a vault order's screen is a Guide level like any other, so back and a restore after process death must treat it so, with the same
 * promise as a draft: only a random id is saved (the number, date and pages live in the encrypted repository), a stored id nobody made is
 * never looked up, and an order the repository no longer holds is cut once it answers.
 */
class GuideNavPart2OrderTest {
    private val bundle = SyntheticPart2Lookup.bundle()
    private val without = SyntheticGuideBundle.withPart2().copy(part2 = null)
    private val id = "7a1d9c34-5b2e-4f80-9d63-0e4b8a21c7f5"

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    @Test
    fun anOrderLevelRoundTripsAsAPlainStringWithTheIdAndNothingElse() {
        val stack = listOf(Part2Destination.Home, Part2Destination.Order(id))
        val saved = save(GuideNavState(stack)) as List<*>
        assertEquals(listOf("part2|home", "part2|order|$id"), saved)
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aMalformedOrderEntryIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2|order", "part2|order|", "part2|order|a|b")) {
            assertEquals(listOf(GuideDestination.Home), GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!.stack, "'$bad' must not restore, nor the card above it")
        }
    }

    @Test
    fun anOrderLevelIsKnownOnlyWithAPartTwoSectionAndAPlainId() {
        assertTrue(bundle.knows(Part2Destination.Order(id)))
        assertFalse(without.knows(Part2Destination.Order(id)), "no Part II section, no order level")
        for (bad in listOf("a b", "<x>", "a|b", "x".repeat(Part2Limits.ID_MAX + 1), "../x")) {
            assertFalse(bundle.knows(Part2Destination.Order(bad)), "'$bad' is not an id the app made")
        }
    }

    @Test
    fun aRestoredStackIsCutAtAnOrderLevelThatCannotBeKnown() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Order("a b")))
        nav.retainKnown(bundle)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home), nav.stack)
    }

    @Test
    fun onlyAnOrderLevelHasAnOrderIdAndOnlyADraftLevelHasADraftId() {
        assertEquals(id, Part2Destination.Order(id).orderId)
        assertNull(Part2Destination.Draft(id).orderId)
        assertNull(Part2Destination.Home.orderId)
        assertNull(Part2Destination.Order(id).draftId, "an order is not a draft: leaving the draft levels for it closes the draft's form")
    }

    @Test
    fun cuttingAnOrderRemovesItsLevelAndOnlyThat() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Order(id)))
        nav.cutOrder(id)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home), nav.stack)
        val other = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Order("another-id")))
        other.cutOrder(id)
        assertEquals(Part2Destination.Order("another-id"), other.current)
    }
}
