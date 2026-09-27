package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import kotlin.test.Test

/**
 * [payAuditFixationCalculatorItems] (docs/Plan/09_PayAudit_PhasePlan.md Phase 6, P7-19b): the pay-fixation
 * calculator section, untested at the UI layer until now. The card's default state (promoted to Level 11
 * in January 2026) already resolves against a fixture timeline resolved at Level 10 in an earlier month,
 * so a comparison renders with no simulated user interaction needed.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditFixationCalculatorSectionTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTheEmptyStateWhenNoMonthHasAResolvedLevelAndStage() =
        runComposeUiTest {
            val timeline =
                ServiceTimeline(
                    months = listOf(TimelineMonth(PayMonth(2023, 1), 71100.0, level = null, stage = null, daPercent = 42, tptaCity = null, occupiesQuarters = false)),
                    events = emptyList(),
                    postings = emptyList(),
                )
            setContent { LazyColumn { payAuditFixationCalculatorItems(timeline = timeline) } }

            onNodeWithText("Upload more payslips to use the pay-fixation calculator.").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersAComparisonWhenTheTimelineResolvesALevelAndStage() =
        runComposeUiTest {
            val timeline =
                ServiceTimeline(
                    months = listOf(TimelineMonth(PayMonth(2023, 1), 71100.0, PayLevel.L10, 5, 42, tptaCity = null, occupiesQuarters = false)),
                    events = emptyList(),
                    postings = emptyList(),
                )
            setContent { LazyColumn { payAuditFixationCalculatorItems(timeline = timeline) } }

            onNodeWithText("Option 1 — fixed pay", substring = true).assertExists()
            onNodeWithText("Option 2 — fixed pay", substring = true).assertExists()
            onNodeWithText("Recommended: Option", substring = true).assertExists()
        }
}
