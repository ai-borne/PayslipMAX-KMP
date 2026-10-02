package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pay Audit lives inside the paid tools list as a ribbon, carrying this month's findings count. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class PremiumToolsSectionBadgeTest {
    @Test
    fun payAuditRibbonShowsTheFindingsCountWhenThereAreFindings() =
        runComposeUiTest {
            setContent { PremiumToolsSection(onNavigateTo = {}, payAuditFindings = 2) }

            onNodeWithText(PayAuditStrings.screenTitle).assertIsDisplayed()
            onNodeWithText("2 findings").assertIsDisplayed()
        }

    @Test
    fun payAuditRibbonShowsNoBadgeOnACleanMonth() =
        runComposeUiTest {
            setContent { PremiumToolsSection(onNavigateTo = {}, payAuditFindings = 0) }

            onNodeWithText(PayAuditStrings.screenTitle).assertIsDisplayed()
            onNodeWithText("0 findings").assertDoesNotExist()
        }

    @Test
    fun tappingThePayAuditRibbonOpensPayAudit() =
        runComposeUiTest {
            var opened: Screen? = null
            setContent { PremiumToolsSection(onNavigateTo = { opened = it }, payAuditFindings = 0) }

            // Like every tool ribbon, the Open button is the tap target; Pay Audit is the first ribbon.
            onAllNodesWithText(InsightsStrings.premiumToolsOpenLabel).onFirst().performClick()

            assertEquals(Screen.PayAudit, opened)
        }
}
