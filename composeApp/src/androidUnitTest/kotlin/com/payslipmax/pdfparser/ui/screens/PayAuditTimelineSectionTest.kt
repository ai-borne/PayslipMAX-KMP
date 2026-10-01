package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.timeline.ChangeExplanation
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import kotlin.test.Test

/**
 * [payAuditChangesItems] / [payAuditHistoryItems] (docs/Plan Phase 2 U3/U4/U5). Adapted from the Phase 4/8
 * tests: the raw "5/2026 — ₹85300 → ₹87800" row is now a named pay line with grouped rupees, and the
 * one-row-per-month timeline and flat "every change" list became spans and year groups.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditTimelineSectionTest {
    private val explainedChange =
        ChangeExplanation(month = PayMonth(2026, 5), field = "basicPay", from = 85300.0, to = 87800.0, reason = "DNI increment.")
    private val olderChange =
        ChangeExplanation(month = PayMonth(2025, 1), field = "transportAllowance", from = 0.0, to = 4212.0, reason = "Posting change ended.")
    private val unexplainedChange =
        ChangeExplanation(month = PayMonth(2026, 5), field = "licenseFee", from = 1200.0, to = 1400.0, reason = null)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun changeRowNamesThePayLineAndGroupsTheRupees() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditChangesItems(changes = listOf(explainedChange, unexplainedChange)) } }

            onNodeWithText("Basic pay").assertExists()
            onNodeWithText("₹85,300 → ₹87,800").assertExists()
            onNodeWithText("DNI increment.").assertExists()
            onNodeWithText("Licence fee").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun changesItemsRendersTheEmptyStateWhenNothingIsExplained() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditChangesItems(changes = listOf(unexplainedChange)) } }

            onNodeWithText("No pay line changed this month.").assertExists()
        }

    private val timeline =
        ServiceTimeline(
            months =
                listOf(
                    TimelineMonth(PayMonth(2025, 12), 80000.0, PayLevel.L11, 4, 50, null, false),
                    TimelineMonth(PayMonth(2026, 1), 85300.0, PayLevel.L11, 5, 50, null, false),
                    TimelineMonth(PayMonth(2026, 2), 85300.0, PayLevel.L11, 5, 50, null, false),
                ),
            events = emptyList(),
            postings = emptyList(),
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun historyShowsSpansInsteadOfOneRowPerMonth() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditHistoryItems(timeline, emptyList(), currentMonth = null) } }

            onNodeWithText("Level 11 · Stage 5").assertExists()
            onNodeWithText("Jan 2026 – Feb 2026").assertExists()
            onNodeWithText("Level 11 · Stage 4").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun everyChangeIsCollapsedByYearExceptTheCurrentOneAndExpandsOnTap() =
        runComposeUiTest {
            setContent {
                LazyColumn { payAuditHistoryItems(ServiceTimeline(emptyList(), emptyList(), emptyList()), listOf(explainedChange, olderChange), currentMonth = PayMonth(2026, 5)) }
            }

            onNodeWithText("DNI increment.").assertExists()
            onNodeWithText("Posting change ended.").assertDoesNotExist()
            onNodeWithText("2025 · 1 change").performClick()
            onNodeWithText("Posting change ended.").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun historyRendersTheEmptyStateWithNoMonths() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditHistoryItems(ServiceTimeline(emptyList(), emptyList(), emptyList()), emptyList(), null) } }

            onNodeWithText("Upload more payslips to build your service timeline.").assertExists()
        }
}
