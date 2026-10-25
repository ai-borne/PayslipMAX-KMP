package com.payslipmax.pdfparser

import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the Dashboard card is the only way into the Due list from outside the Guide, so one tap must both set the Guide's stack to Part II's
 * Due and bring the Guide tab up, and must do neither when the Guide is off (a build without the Guide has no card, but the helper is the
 * last guard).
 */
class OpenDueTest {
    @Test
    fun itSetsTheGuideStackToDueAndSwitchesToTheGuideTabOnce() {
        val guide = GuideNavState(listOf(GuideDestination.Search))
        val shown = mutableListOf<Screen>()

        openDue(guide) { shown += it }

        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Due), guide.stack)
        assertEquals(listOf(Screen.Guide), shown)
    }

    @Test
    fun withoutTheGuideItDoesNothing() {
        val shown = mutableListOf<Screen>()

        openDue(null) { shown += it }

        assertEquals(emptyList(), shown)
    }
}
