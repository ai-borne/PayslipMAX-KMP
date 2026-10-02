package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The verdict wording is the phase's acceptance gate: pay line + rupee amount for an issue, never alarming for waiting. */
class PayAuditVerdictCopyTest {
    private val aug = PayMonth(2026, 8)

    @Test
    fun cleanMonthSaysNoIssuesWithoutClaimingEveryLineWasAudited() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Clean(12, 0), aug)
        assertEquals("Aug 2026: no issues found on 12 pay lines", c.headline)
        assertEquals(VerdictTone.OK, c.tone)
    }

    @Test
    fun singleIssueNamesThePayLineAndTheIndianGroupedAmount() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Issue(1, listOf("TPTA (transport allowance)"), 5508.0, canDraftLetter = true), aug)
        assertEquals("TPTA (transport allowance) ₹5,508 short", c.headline)
        assertEquals(VerdictTone.ISSUE, c.tone)
        assertEquals("See why and draft a letter", c.cta)
    }

    @Test
    fun issueWithoutAProvenLetterOffersTheEvidenceNotADraft() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Issue(1, listOf("Basic pay"), 100.0, canDraftLetter = false), aug)
        assertEquals("See the evidence", c.cta)
    }

    @Test
    fun severalIssuesListPayLinesWithoutInventingATotal() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Issue(2, listOf("TPTA (transport allowance)", "Basic pay"), null, canDraftLetter = false), aug)
        assertEquals("2 issues: TPTA (transport allowance), Basic pay", c.headline)
        assertFalse(c.headline.contains("₹"))
    }

    @Test
    fun lockedIssueKeepsThePayLineButHidesTheAmount() {
        val c = payAuditVerdictCopy(PayAuditVerdict.LockedIssue(1, listOf("TPTA (transport allowance)")), aug)
        assertEquals("Issue: TPTA (transport allowance)", c.headline)
        assertFalse(c.headline.contains("₹"))
        assertEquals("Unlock the amount", c.cta)
    }

    @Test
    fun waitingIsNotRedAndHasNoAction() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Waiting(1), aug)
        assertEquals("1 waiting for your next payslip", c.headline)
        assertEquals(VerdictTone.WAITING, c.tone)
        assertEquals(null, c.cta)
    }

    @Test
    fun verifiedArrearsAreMentionedOnACleanVerdictAsGoodNews() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Clean(14, 2), aug)
        assertEquals(VerdictTone.OK, c.tone)
        assertEquals("Arrears credits verified, they match the rules exactly: 2", c.subtitle)
    }

    @Test
    fun historyLineNeverCountsWaiting() {
        assertEquals("33 months audited · 0 issues", payAuditHistoryLine(PayAuditHistorySummary(33, 0)))
        assertEquals("20 months audited · 1 issue", payAuditHistoryLine(PayAuditHistorySummary(20, 1)))
        assertEquals("20 months audited · 3 issues", payAuditHistoryLine(PayAuditHistorySummary(20, 3)))
    }

    // WHY: found on the Pixel with the Phase 8 seed: a held TPTA gap said "Arrears are not credited yet", which is
    // not what is being waited for (a posting change or relocation to be ruled out by the next payslip).
    @Test
    fun aWaitingMissingPayLineIsNotDescribedAsArrears() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Waiting(1, forMissingPayLine = true), aug)
        assertFalse(c.subtitle.contains("Arrears"), c.subtitle)
        assertEquals("Aug 2026. A missing pay line can be explained by a posting change or relocation. We check again when your next payslip is added. Not counted as an issue.", c.subtitle)
    }

    @Test
    fun aWaitingArrearsKeepsTheArrearsWording() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Waiting(1), aug)
        assertEquals("Aug 2026. Arrears are not credited yet. They normally arrive 1–3 months later. Not counted as an issue.", c.subtitle)
    }

    @Test
    fun theVerdictKnowsWhetherTheWaitingFindingIsAMissingPayLineOrArrears() {
        val tpta = Anomaly("TPTA_ENTITLEMENT", "transportAllowance", 4212.0, "03/2018", "held", isPending = true)
        val arrears = tpta.copy(field = "arrearsDa")
        assertEquals(PayAuditVerdict.Waiting(1, forMissingPayLine = true), buildPayAuditVerdict(PayAuditMonthFindings(waiting = listOf(tpta)), true, 3))
        assertEquals(PayAuditVerdict.Waiting(1), buildPayAuditVerdict(PayAuditMonthFindings(waiting = listOf(arrears)), true, 3))
        assertEquals(PayAuditVerdict.Waiting(2), buildPayAuditVerdict(PayAuditMonthFindings(waiting = listOf(tpta, arrears)), true, 3), "any arrears keeps the arrears wording")
    }
}
