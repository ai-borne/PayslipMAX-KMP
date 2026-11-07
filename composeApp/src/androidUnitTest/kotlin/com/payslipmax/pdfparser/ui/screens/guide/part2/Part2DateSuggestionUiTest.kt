package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDateBox
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: a suggested date only helps if one tap on Set takes it, and never if the officer did not choose it: the box keeps saying
 * "Choose a date" until he confirms, an existing date always beats the suggestion, and a box with no suggestion opens empty as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class Part2DateSuggestionUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var picked: CalendarDate? = CalendarDate(1999, 1, 1)

    private fun show(
        date: CalendarDate?,
        suggested: CalendarDate?,
    ) {
        picked = CalendarDate(1999, 1, 1)
        composeRule.setContent { PreClaimDateBox("From", date, { picked = it }, suggested) }
    }

    private fun openAndSet() {
        composeRule.onNodeWithText("From", substring = true).performClick()
        composeRule.onNodeWithText(PreClaimStrings.dateSet).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun settingStraightAwayTakesTheSuggestedDate() {
        show(date = null, suggested = CalendarDate(2026, 11, 20))

        openAndSet()

        assertEquals(CalendarDate(2026, 11, 20), picked)
    }

    @Test
    fun theBoxStillSaysChooseADateUntilTheOfficerConfirms() {
        show(date = null, suggested = CalendarDate(2026, 11, 20))

        composeRule.onNodeWithText("From: ${PreClaimStrings.dateChoose}").assertExists()
    }

    @Test
    fun aDateAlreadyChosenBeatsTheSuggestion() {
        show(date = CalendarDate(2026, 3, 1), suggested = CalendarDate(2026, 11, 20))

        openAndSet()

        assertEquals(CalendarDate(2026, 3, 1), picked)
    }

    @Test
    fun noSuggestionOpensEmptyAsBefore() {
        show(date = null, suggested = null)

        openAndSet()

        assertNull(picked)
    }
}
