package com.payslipmax.pdfparser.ui.screens.guide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: Learn is two things. Opened from the hub it teaches; opened from "Add a casualty" on the attach form it is a picker for one order.
 * The picker must know which, from the stack alone (no new saved state), and must agree with where a saved Add returns to.
 */
class GuideNavStartedFromAttachTest {
    private val learn = arrayOf(Part2Destination.Home, Part2Destination.Ribbon("leave"))

    private fun nav(vararg above: GuideDestination) = GuideNavState(above.toList())

    @Test
    fun learnOpenedFromTheAttachFormIsAPickerForThatOrder() {
        assertTrue(nav(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach(), *learn).startedFromAttach)
        assertTrue(nav(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1"), Part2Destination.Attach("e1"), *learn).startedFromAttach)
    }

    @Test
    fun learnOpenedFromTheHubOrTheBankIsJustLearn() {
        assertFalse(nav(Part2Destination.Hub, *learn).startedFromAttach)
        assertFalse(nav(Part2Destination.Hub, Part2Destination.Bank, *learn).startedFromAttach)
    }

    @Test
    fun anEntryFormOpenedFromAnExistingEntryOfTheBankIsNotTheAttachContext() {
        assertFalse(nav(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1")).startedFromAttach)
    }

    @Test
    fun itAgreesWithWhereASavedAddReturnsTo() {
        val stacks =
            listOf(
                arrayOf(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach(), *learn, Part2Destination.Entry("new")),
                arrayOf(Part2Destination.Hub, *learn, Part2Destination.Entry("new")),
            )
        stacks.forEach { stack -> assertEquals(nav(*stack).startedFromAttach, nav(*stack).landAfterAdd()) }
    }
}
