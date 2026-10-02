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
 * [payAuditFixationCalculatorItems] (docs/Plan/09_PayAudit_PhasePlan.md Phase 6, P7-19b). The card's
 * default `toLevel`/promotion-month state is derived from the officer's own latest trusted timeline month
 * (one level up, one month later) rather than hardcoded, so a comparison renders on first paint for any
 * officer — a hardcoded "Level 11 / January 2026" default silently broke on a real device for an officer
 * already at Level 12A, or whose latest payslip was already past January 2026 (caught 2026-09-28 driving
 * the Pixel 9 test device, fixed same day).
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersAComparisonByDefaultForAnOfficerAlreadyAtOrAboveTheOldHardcodedLevelEleven() =
        runComposeUiTest {
            // Regression for the Pixel 9 bug: the real device's officer was at Level 12A with a latest
            // payslip in 05/2026 — the old hardcoded default ("promoted to Level 11 in 01/2026") failed
            // both of resolveFixationComparison's own checks (toLevel not higher than current; promotion
            // month not after the latest trusted month) and showed the invalid-input error on first paint.
            val timeline =
                ServiceTimeline(
                    months = listOf(TimelineMonth(PayMonth(2026, 5), 141200.0, PayLevel.L12A, 8, 60, tptaCity = null, occupiesQuarters = false)),
                    events = emptyList(),
                    postings = emptyList(),
                )
            setContent { LazyColumn { payAuditFixationCalculatorItems(timeline = timeline) } }

            onNodeWithText("Enter a valid promotion year and a level higher than your current one.").assertDoesNotExist()
            onNodeWithText("Option 1 — fixed pay", substring = true).assertExists()
        }
}
