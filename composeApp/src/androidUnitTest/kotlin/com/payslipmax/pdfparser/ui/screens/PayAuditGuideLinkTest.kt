package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * E7: the "Read the rule" link on a Pay Audit finding. It appears only when the host supplies a handler (the Guide is on) and
 * only for a (type, pay line) the owner mapped to a card; it never replaces or hides the existing actions.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditGuideLinkTest {
    private val tpta =
        Anomaly("TPTA_ENTITLEMENT", "transportAllowance", 4212.0, "05/2026", "TPTA is missing.", expected = 4212.0, actual = 0.0, authority = "GoI MoD letter")
    private val verifiedArrears =
        Anomaly("ARREARS_AUDIT", "arrearsDa", 9870.0, "04/2026", "Verified: matches exactly.", expected = 9870.0, actual = 9870.0)
    private val unmapped = tpta.copy(type = "MISSING_ALLOWANCE", field = "transportAllowance", description = "Not mapped by the owner.")

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun anIssueOpensItsApprovedCardAndKeepsTheOtherActions() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(issues = listOf(tpta)), 0, {}, {}, onOpenGuideCard = { opened += it }) }
            }

            onNodeWithText(GuideStrings.payAuditSeeRule).performClick()

            assertEquals(listOf("RB-SS-P051-rates"), opened)
            onNodeWithText("Why?").assertExists()
            onNodeWithText("Draft letter").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aWaitingFindingLinksToo() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(waiting = listOf(tpta.copy(isPending = true))), 0, {}, {}, onOpenGuideCard = { opened += it }) }
            }

            onNodeWithText(GuideStrings.payAuditSeeRule).performClick()

            assertEquals(listOf("RB-SS-P051-rates"), opened)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aVerifiedArrearsRowLinksToTheDaRevisionCard() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            setContent {
                LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(verified = listOf(verifiedArrears)), 0, {}, {}, onOpenGuideCard = { opened += it }) }
            }

            onNodeWithText(GuideStrings.payAuditSeeRule).performClick()

            assertEquals(listOf("RB-RP-052"), opened)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withTheGuideOffThereIsNoLinkAtAll() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(issues = listOf(tpta)), 0, {}, {}, onOpenGuideCard = null) } }

            onNodeWithText(GuideStrings.payAuditSeeRule).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aFindingTheOwnerDidNotMapHasNoLinkEvenWithTheGuideOn() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditFindingsItems(PayAuditMonthFindings(issues = listOf(unmapped)), 0, {}, {}, onOpenGuideCard = {}) } }

            onNodeWithText(GuideStrings.payAuditSeeRule).assertDoesNotExist()
        }
}
