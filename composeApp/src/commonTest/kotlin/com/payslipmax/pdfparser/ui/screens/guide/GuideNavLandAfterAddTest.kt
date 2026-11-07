package com.payslipmax.pdfparser.ui.screens.guide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: one Part II order often covers several casualties. An officer half way through "Attach an order" who taps "Add a casualty" came
 * to add one more casualty to THAT order, so a saved Add must put him back on that form (scan and fields kept, the new casualty ticked),
 * not on a fresh Bank, which also closed the form and threw his scan away. Any other Add still lands on the Bank.
 */
class GuideNavLandAfterAddTest {
    private val home = GuideDestination.Home
    private val trail = arrayOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Code("GTDAL"), Part2Destination.Entry("new"))

    private fun land(vararg above: GuideDestination): Pair<Boolean, List<GuideDestination>> = GuideNavState(above.toList()).let { it.landAfterAdd() to it.stack }

    @Test
    fun anAddStartedFromTheAttachFormReturnsToThatFormWithTheLearnTrailGone() {
        val (onForm, stack) = land(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach(), *trail)

        assertTrue(onForm)
        assertEquals(listOf(home, Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach()), stack)
    }

    @Test
    fun anAttachFormOpenedFromAnEntryIsReturnedToWithThatEntryBelowIt() {
        val (onForm, stack) = land(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1"), Part2Destination.Attach("e1"), *trail)

        assertTrue(onForm)
        assertEquals(listOf(home, Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1"), Part2Destination.Attach("e1")), stack)
    }

    @Test
    fun anAddWithNoAttachFormOpenStillLandsOnTheBank() {
        val (onForm, stack) = land(Part2Destination.Hub, *trail)

        assertFalse(onForm)
        assertEquals(listOf(home, Part2Destination.Hub, Part2Destination.Bank), stack)
    }
}
