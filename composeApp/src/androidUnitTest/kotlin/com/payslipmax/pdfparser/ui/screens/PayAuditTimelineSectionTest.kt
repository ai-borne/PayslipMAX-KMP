package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.timeline.ChangeExplanation
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import com.payslipmax.pdfparser.insights.timeline.TptaCityClass
import kotlin.test.Test

/**
 * [payAuditChangesItems]/[payAuditAllChangesItems]/[payAuditTimelineItems] (docs/Plan/09_PayAudit_PhasePlan.md
 * Phase 4/8, P7-19b): the free "What changed this month"/"Every change explained"/Service Timeline
 * sections of [PayAuditScreen], untested at the UI layer until now.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditTimelineSectionTest {
    private val explainedChange =
        ChangeExplanation(month = PayMonth(2026, 5), field = "basicPay", from = 85300.0, to = 87800.0, reason = "DNI increment.")
    private val olderExplainedChange =
        ChangeExplanation(month = PayMonth(2026, 1), field = "transportAllowance", from = 0.0, to = 4212.0, reason = "Posting change ended.")
    private val unexplainedChange =
        ChangeExplanation(month = PayMonth(2026, 5), field = "licenseFee", from = 1200.0, to = 1400.0, reason = null)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun changesItemsRendersOnlyExplainedMoves() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditChangesItems(changes = listOf(explainedChange, unexplainedChange)) } }

            onNodeWithText("5/2026 — ₹85300 → ₹87800", substring = true).assertExists()
            onNodeWithText("DNI increment.").assertExists()
            onNodeWithText("No pay-line changes are explained for this month yet.").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun changesItemsRendersTheEmptyStateWhenNothingIsExplained() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditChangesItems(changes = listOf(unexplainedChange)) } }

            onNodeWithText("No pay-line changes are explained for this month yet.").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun allChangesItemsExcludesTheCurrentMonthAndSortsNewestFirst() =
        runComposeUiTest {
            setContent {
                LazyColumn {
                    payAuditAllChangesItems(changes = listOf(explainedChange, olderExplainedChange), currentMonth = PayMonth(2026, 5))
                }
            }

            onNodeWithText("Posting change ended.").assertExists()
            onNodeWithText("DNI increment.").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timelineItemsRendersMonthsNewestFirstWithLevelStageAndDa() =
        runComposeUiTest {
            val timeline =
                ServiceTimeline(
                    months =
                        listOf(
                            TimelineMonth(PayMonth(2026, 1), 85300.0, PayLevel.L11, 5, 50, TptaCityClass.HIGHER, occupiesQuarters = true),
                        ),
                    events = emptyList(),
                    postings = emptyList(),
                )
            setContent { LazyColumn { payAuditTimelineItems(timeline = timeline) } }

            onNodeWithText("1/2026").assertExists()
            onNodeWithText("Level 11, Stage 5 · 50% DA · TPTA (higher-rate city) · Quarters", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timelineItemsRendersTheEmptyStateWithNoMonths() =
        runComposeUiTest {
            val timeline = ServiceTimeline(months = emptyList(), events = emptyList(), postings = emptyList())
            setContent { LazyColumn { payAuditTimelineItems(timeline = timeline) } }

            onNodeWithText("Upload more payslips to build your service timeline.").assertExists()
        }
}
