package com.payslipmax.pdfparser.ui.screens.guide

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: an officer who adds a casualty came to fill the Orders Bank, so a saved Add must land on the Bank with nothing of the Learn trail
 * (search, ribbon, casualty page) left under Back. Left on the casualty page, the Add button was still under the thumb and a second tap
 * made a second blank form. Back from the Bank is the hub it was reached from, one level at a time.
 */
class GuideNavLandOnBankTest {
    private val home = GuideDestination.Home
    private val entry = Part2Destination.Entry("e-1")
    private val trail = listOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Code("GTDAL"))

    private fun landed(vararg above: GuideDestination): List<GuideDestination> = GuideNavState(above.toList()).also { it.landOnBank() }.stack

    @Test
    fun anAddStartedFromTheBankGoesBackToTheBankItCameFrom() {
        assertEquals(
            listOf(home, Part2Destination.Hub, Part2Destination.Bank),
            landed(Part2Destination.Hub, Part2Destination.Bank, *trail.toTypedArray(), entry),
        )
    }

    @Test
    fun anAddStartedFromTheHubPutsTheBankOnTheHubNotOnTheLearnTrail() {
        assertEquals(
            listOf(home, Part2Destination.Hub, Part2Destination.Bank),
            landed(Part2Destination.Hub, *trail.toTypedArray(), entry),
        )
    }

    @Test
    fun relatedCasualtyPagesOpenedOnTheWayAreAllLeftBehind() {
        assertEquals(
            listOf(home, Part2Destination.Hub, Part2Destination.Bank),
            landed(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("GTDAL"), Part2Destination.Code("EXTNAL"), entry),
        )
    }

    @Test
    fun aStackRestoredWithoutTheHubStillEndsOnTheBankAboveGuideHome() {
        assertEquals(listOf(home, Part2Destination.Bank), landed(Part2Destination.Home, Part2Destination.Code("GTDAL"), entry))
    }

    @Test
    fun levelsBelowTheTrailAreKeptAsTheyWere() {
        assertEquals(
            listOf(home, Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Shelf, Part2Destination.Bank),
            landed(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Shelf, Part2Destination.Bank, Part2Destination.Home, entry),
        )
    }

    @Test
    fun theBanksOwnScrollPlaceSurvivesTheLanding() {
        val nav = GuideNavState(listOf(Part2Destination.Hub, Part2Destination.Bank, *trail.toTypedArray(), entry))
        nav.saveScroll(GuideScroll(7, 40), level = 2)

        nav.landOnBank()

        assertEquals(GuideScroll(7, 40), nav.currentScroll, "the list is where it was left; the new row is then scrolled to by the screen")
    }
}
