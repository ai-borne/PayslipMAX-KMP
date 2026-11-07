package com.payslipmax.pdfparser.ui.screens.guide

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: after scanning an order the officer is four screens deep (Bank, entry, attach, order). The "Orders Bank" shortcut must put the
 * Bank list under his thumb in one tap, whatever he opened on the way, and must never leave a level above the Bank to walk back through.
 */
class GuideNavBackToBankTest {
    private val home = GuideDestination.Home
    private val bank = Part2Destination.Bank

    private fun back(vararg stack: GuideDestination): List<GuideDestination> = GuideNavState(stack.toList()).also { it.backToBank() }.stack

    @Test
    fun fromTheOrderTheBankIsOneTapAwayWithTheEntryAndAttachLevelsGone() {
        assertEquals(listOf(home, Part2Destination.Hub, bank), back(Part2Destination.Hub, bank, Part2Destination.Entry("e1"), Part2Destination.Attach("e1"), Part2Destination.Order("o1")))
    }

    @Test
    fun anOrderOpenedFromTheShelfStillGoesStraightToTheBankNotBackThroughTheShelf() {
        assertEquals(listOf(home, Part2Destination.Hub, bank), back(Part2Destination.Hub, bank, Part2Destination.Shelf, Part2Destination.Order("o1"), Part2Destination.Entry("e1")))
    }

    @Test
    fun anEntryOpenedFromTheLearnPagesGetsAFreshBankAndLeavesTheTrailBehind() {
        assertEquals(listOf(home, Part2Destination.Hub, bank), back(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("GTDAL"), Part2Destination.Entry("e1")))
    }

    @Test
    fun theBankAloneStaysAsItWas() {
        assertEquals(listOf(home, Part2Destination.Hub, bank), back(Part2Destination.Hub, bank))
    }
}
