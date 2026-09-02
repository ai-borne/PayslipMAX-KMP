package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payslipmax.pdfparser.domain.SalaryCountdownUiModel
import com.payslipmax.pdfparser.ui.theme.PDFParserTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SalaryCountdownRibbonUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCountdownRibbonRendersNormalDaysLeft() {
        val model =
            SalaryCountdownUiModel(
                daysRemaining = 4,
                paydayDateFormatted = "31 Aug",
                isPaydayToday = false,
                progressRatio = 0.87f,
                currentDay = 27,
                totalDaysInMonth = 31,
            )

        composeTestRule.setContent {
            PDFParserTheme {
                SalaryCountdownRibbon(countdown = model)
            }
        }

        composeTestRule.onNodeWithTag("salary_countdown_ribbon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Payday: 31 Aug").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 Days Left").assertIsDisplayed()
        composeTestRule.onNodeWithTag("salary_countdown_progress").assertIsDisplayed()
    }

    @Test
    fun testCountdownRibbonRendersPaydayCelebration() {
        val model =
            SalaryCountdownUiModel(
                daysRemaining = 0,
                paydayDateFormatted = "31 Aug",
                isPaydayToday = true,
                progressRatio = 1.0f,
                currentDay = 31,
                totalDaysInMonth = 31,
            )

        composeTestRule.setContent {
            PDFParserTheme {
                SalaryCountdownRibbon(countdown = model)
            }
        }

        composeTestRule.onNodeWithTag("salary_countdown_ribbon").assertIsDisplayed()
        composeTestRule.onNodeWithText("🎉 Salary Day Today!").assertIsDisplayed()
    }
}
