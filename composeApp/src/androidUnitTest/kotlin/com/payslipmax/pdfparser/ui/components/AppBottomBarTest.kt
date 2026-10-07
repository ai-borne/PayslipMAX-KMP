package com.payslipmax.pdfparser.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the bottom bar the app shipped before the Claim Guide: four tabs, each routing to its own root. The Guide
 * is a dark launch, so with it off these four must stay exactly as they were.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppBottomBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fourTabs =
        listOf(
            AppStrings.navigationHome to Screen.Dashboard,
            AppStrings.navigationHistory to Screen.History,
            AppStrings.navigationInsights to Screen.Insights,
            AppStrings.navigationSettings to Screen.Settings,
        )

    @Test
    fun theFourOriginalTabsEachNavigateToTheirRoot() {
        val navigated = mutableListOf<Screen>()
        composeRule.setContent { AppBottomBar(currentScreen = Screen.Dashboard, onNavigate = { navigated += it }) }

        fourTabs.forEach { (label, _) -> composeRule.onNodeWithText(label).performClick() }

        assertEquals(fourTabs.map { it.second }, navigated)
    }

    @Test
    fun theFourOriginalTabsAreAllThereIsWhenTheGuideIsOff() {
        composeRule.setContent { AppBottomBar(currentScreen = Screen.Dashboard, onNavigate = {}) }
        composeRule.onNodeWithText(GuideStrings.tabLabel).assertDoesNotExist()
    }

    @Test
    fun theGuideTabSitsBeforeSettingsAndReTapGoesToGuideHome() {
        var current by mutableStateOf(Screen.Dashboard)
        var reselected = 0
        composeRule.setContent {
            AppBottomBar(currentScreen = current, onNavigate = { current = it }, showGuide = true, onGuideReselected = { reselected++ })
        }
        val guideLeft = composeRule.onNodeWithText(GuideStrings.tabLabel).left()
        val settingsLeft = composeRule.onNodeWithText(AppStrings.navigationSettings).left()
        assertTrue(guideLeft < settingsLeft, "Guide sits before Settings (the approved preview)")

        composeRule.onNodeWithText(GuideStrings.tabLabel).performClick()
        assertEquals(Screen.Guide, current)
        assertEquals(0, reselected, "the first tap switches tabs; it is not a re-tap")

        composeRule.onNodeWithText(GuideStrings.tabLabel).performClick()
        assertEquals(1, reselected)
    }

    private fun SemanticsNodeInteraction.left(): Float = fetchSemanticsNode().boundsInRoot.left
}
