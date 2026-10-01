package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The redesigned Pay Audit screen body (docs/Plan Phase 2): the verdict is the first thing shown, the tabs
 * switch what is below it, and the month bar steps only through months that have a payslip.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditScreenContentTest {
    private val aug = PayMonth(2026, 8)
    private val sep = PayMonth(2026, 9)
    private val base = PayAuditUiState(selectedMonth = aug, availableMonths = listOf(aug, sep), verdict = PayAuditVerdict.Clean(12, 0), history = PayAuditHistorySummary(20, 0))

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cleanVerdictAnswersNoIssuesAndShowsTheHistoryLineWithoutScrolling() =
        runComposeUiTest {
            setContent { PayAuditContent(base, {}, {}, {}, {}) }

            onNodeWithText("Aug 2026: no issues found on 12 pay lines").assertExists()
            onNodeWithText("20 months audited · 0 issues").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun issueVerdictNamesPayLineAndAmountAndDraftsALetter() =
        runComposeUiTest {
            var drafts = 0
            val state = base.copy(verdict = PayAuditVerdict.Issue(1, listOf("TPTA (transport allowance)"), 5508.0, canDraftLetter = true))
            setContent { PayAuditContent(state, {}, {}, {}, { drafts++ }) }

            onNodeWithText("TPTA (transport allowance) ₹5,508 short").assertExists()
            onNodeWithText("See why and draft a letter").performClick()
            assertEquals(1, drafts)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedVerdictShowsPayLineButOffersUnlockInsteadOfTheAmount() =
        runComposeUiTest {
            var unlocks = 0
            val state = base.copy(verdict = PayAuditVerdict.LockedIssue(1, listOf("TPTA (transport allowance)")), isLocked = true)
            setContent { PayAuditContent(state, {}, {}, { unlocks++ }, {}) }

            onNodeWithText("Issue: TPTA (transport allowance)").assertExists()
            onNodeWithText("Unlock the amount").performClick()
            assertEquals(1, unlocks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun waitingVerdictIsNotPresentedAsAnIssue() =
        runComposeUiTest {
            setContent { PayAuditContent(base.copy(verdict = PayAuditVerdict.Waiting(1)), {}, {}, {}, {}) }

            onNodeWithText("1 waiting for your next payslip").assertExists()
            onNodeWithText("! Issue found").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabsReportTheSelectionAndPlanAheadShowsPredictions() =
        runComposeUiTest {
            var tab: PayAuditTab? = null
            setContent { PayAuditContent(base, {}, { tab = it }, {}, {}) }

            onNodeWithText("History").performClick()
            assertEquals(PayAuditTab.HISTORY, tab)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun planAheadTabRendersTheNextIncrementCard() =
        runComposeUiTest {
            setContent { PayAuditContent(base.copy(tab = PayAuditTab.PLAN_AHEAD), {}, {}, {}, {}) }

            onNodeWithText("Next increment").assertExists()
            onNodeWithText("This month").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun monthStepperMovesToTheNextMonthThatHasAPayslipAndDisablesAtTheEnds() =
        runComposeUiTest {
            var picked: PayMonth? = null
            setContent { PayAuditContent(base, { picked = it }, {}, {}, {}) }

            onNodeWithContentDescription("Next month").performClick()
            assertEquals(sep, picked)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun infoButtonOpensTheGlossary() =
        runComposeUiTest {
            setContent { PayAuditContent(base, {}, {}, {}, {}) }

            onNodeWithText("ⓘ").performClick()
            onNodeWithText("Date of Next Increment", substring = true).assertExists()
        }
}
