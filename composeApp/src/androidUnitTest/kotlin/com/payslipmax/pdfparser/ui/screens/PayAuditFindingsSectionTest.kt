package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.Anomaly
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [payAuditFindingsItems] (docs/Plan Phase 2 U6). Rewritten from the Phase 4/7 tests: findings are now
 * evidence cards (pay line, should-be / credited / difference, a "Why?" expander) with a "Draft letter"
 * action that must appear only for proven, non-pending findings, and verified arrears render as good news.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditFindingsSectionTest {
    private val proven =
        Anomaly(
            type = "TPTA_ENTITLEMENT",
            field = "transportAllowance",
            amount = 4212.0,
            month = "05/2026",
            description = "Transport Allowance is missing from your earnings ledger.",
            expected = 4212.0,
            actual = 100.0,
            authority = "GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17",
        )
    private val unproven = proven.copy(field = "militaryServicePay", authority = null, description = "No authority yet.")
    private val waiting = proven.copy(field = "arrearsDa", isPending = true, description = "Waiting for a later payslip.")
    private val verified =
        Anomaly("ARREARS_AUDIT", "arrearsDa", 9870.0, "04/2026", "Verified: matches exactly.", expected = 9870.0, actual = 9870.0)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun issueCardNamesThePayLineShowsEvidenceAndRevealsTheAuthorityOnWhy() =
        runComposeUiTest {
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(issues = listOf(proven)), 0, onUnlockClick = {}, onDraftLetter = {}) }
            }

            onNodeWithText("TPTA (transport allowance)").assertExists()
            onNodeWithText("₹4,212").assertExists()
            onNodeWithText("₹4,112").assertExists()
            onNodeWithText("Authority:", substring = true).assertDoesNotExist()
            onNodeWithText("Why?").performClick()
            onNodeWithText("Authority: GoI MoD letter", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun draftLetterActionAppearsOnlyForAProvenFindingAndOpensTheLetterFlow() =
        runComposeUiTest {
            var drafts = 0
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(issues = listOf(proven)), 0, onUnlockClick = {}, onDraftLetter = { drafts++ }) }
            }
            onNodeWithText("Draft letter").performClick()
            assertEquals(1, drafts)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun noDraftLetterForUnprovenOrWaitingFindings() =
        runComposeUiTest {
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(issues = listOf(unproven), waiting = listOf(waiting)), 0, {}, {}) }
            }
            onNodeWithText("Military Service Pay (MSP)").assertExists()
            onNodeWithText("Draft letter").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun verifiedArrearsStayVisibleAsGoodNewsNotAsAnIssue() =
        runComposeUiTest {
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(verified = listOf(verified)), 0, {}, {}) }
            }
            onNodeWithText("Verified · not an issue").assertExists()
            onNodeWithText("Issue · Proven").assertDoesNotExist()
            onNodeWithText("Draft letter").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun waitingCardSaysItIsCheckedAgainWhenTheNextPayslipArrives() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(waiting = listOf(waiting)), 0, {}, {}) } }
            onNodeWithText("We check again", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedCardOffersUnlockAndLeaksNoDescription() =
        runComposeUiTest {
            var unlocks = 0
            setContent { LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(), hiddenCount = 1, onUnlockClick = { unlocks++ }, onDraftLetter = {}) } }
            onNodeWithText("Transport Allowance is missing from your earnings ledger.").assertDoesNotExist()
            onNodeWithText("Unlock evidence and letter").performClick()
            assertEquals(1, unlocks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun twoFindingsSharingTypeAndMonthDoNotCrashTheList() =
        runComposeUiTest {
            // Regression for commit 8ce8e766: the basic-DA and TPTA-DA arrears checks share (type, month).
            val a = verified
            val b = verified.copy(description = "Verified: TPTA DA matches exactly.")
            setContent { LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(verified = listOf(a, b)), 0, {}, {}) } }
            onNodeWithText("Verified: matches exactly.").assertExists()
        }
}
