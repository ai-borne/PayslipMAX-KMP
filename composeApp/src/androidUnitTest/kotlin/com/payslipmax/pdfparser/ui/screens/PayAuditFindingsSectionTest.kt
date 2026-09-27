package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.Anomaly
import kotlin.test.Test

/**
 * [payAuditFindingsItems] (docs/Plan/09_PayAudit_PhasePlan.md Phase 4/Phase 7 P7-08): unlocked findings
 * render the evidence ([Anomaly.expected]/[Anomaly.actual]/[Anomaly.authority]) attached in Phase 2, and
 * a locked (unproven-tier) display shows the count/CTA teaser without leaking any finding's description.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditFindingsSectionTest {
    private val anomalyWithEvidence =
        Anomaly(
            type = "TPTA_ENTITLEMENT",
            field = "transportAllowance",
            amount = 4212.0,
            month = "05/2026",
            description = "Transport Allowance is missing from your earnings ledger.",
            expected = 4212.0,
            actual = 0.0,
            authority = "GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17",
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersUnlockedFindingWithItsEvidenceAndAuthority() =
        runComposeUiTest {
            val display = PayAuditFindingsDisplay(unlocked = listOf(anomalyWithEvidence), lockedCount = 0, lockedLabels = emptyList())
            setContent { LazyColumn { payAuditFindingsItems(display = display, onUnlockClick = {}) } }

            onNodeWithText("Transport Allowance is missing from your earnings ledger.").assertExists()
            onNodeWithText("Expected: ₹4212   Actual: ₹0", substring = true).assertExists()
            onNodeWithText("Authority: GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersLockedTeaserWithoutLeakingTheFindingDescription() =
        runComposeUiTest {
            val display = PayAuditFindingsDisplay(unlocked = emptyList(), lockedCount = 1, lockedLabels = listOf("Allowance"))
            setContent { LazyColumn { payAuditFindingsItems(display = display, onUnlockClick = {}) } }

            onNodeWithText("Unlock Pay Audit findings").assertExists()
            onNodeWithText("Transport Allowance is missing from your earnings ledger.").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTheEmptyStateWhenThereAreNoFindings() =
        runComposeUiTest {
            val display = PayAuditFindingsDisplay(unlocked = emptyList(), lockedCount = 0, lockedLabels = emptyList())
            setContent { LazyColumn { payAuditFindingsItems(display = display, onUnlockClick = {}) } }

            onNodeWithText("No findings on this payslip — everything checks out.").assertExists()
        }
}
