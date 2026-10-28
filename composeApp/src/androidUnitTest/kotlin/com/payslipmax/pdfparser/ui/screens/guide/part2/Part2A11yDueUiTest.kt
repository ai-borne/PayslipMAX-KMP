package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.ui.theme.Part2Strings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (Phase 11 item 35): the Due list is what a reminder opens, so it is the screen an officer reaches in a hurry. Same checks as
 * [Part2A11yUiTest] for the Part II home with its Due row and for the list with an overdue item, on the shipped bundle. Synthetic dates.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2A11yDueUiTest : Part2DueUiBase() {
    private val checks by lazy { Part2A11yChecks(composeRule) }

    private fun screens(onScreen: (String) -> Unit) {
        hold("d1")
        hold("d2", firstDay = 14, lastDay = 20)
        show()
        tap(Part2Strings.entryTitle)
        onScreen("the Part II home with a Due row")
        tap(com.payslipmax.pdfparser.ui.theme.Part2DueStrings.rowDue(1))
        onScreen("the Due list")
    }

    @Test
    fun everyControlHasANameAndNoTwoTouchAreasOverlap() {
        val faults = mutableListOf<String>()
        screens { screen -> faults += (checks.unnamedActions() + checks.crowdedTargets()).map { "$screen: $it" } }
        assertEquals(emptyList(), faults)
    }

    @Test
    fun atTwiceTheFontNothingRunsOffTheEdgeOrLosesAButtonsLabel() {
        fontScale = 2f
        val faults = mutableListOf<String>()
        screens { screen -> faults += checks.largeFontFaults().map { "$screen: $it" } }
        assertEquals(emptyList(), faults)
    }
}
