package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The verdict wording is the phase's acceptance gate: pay line + rupee amount for an issue, never alarming for waiting. */
class PayAuditVerdictCopyTest {
    private val aug = PayMonth(2026, 8)

    @Test
    fun cleanMonthStatesTheLinesCheckedAndIsGreen() {
        val c = payAuditVerdictCopy(PayAuditVerdict.Clean(12, 0), aug)
        assertEquals("Aug 2026: 12 pay lines checked, all correct", c.headline)
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
}
