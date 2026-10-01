package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DsopFund
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pay Audit Phase 2: the ViewModel owns the engine run, the selected month and the verdict. These tests
 * pin *why* the verdict matters: the user must be told "correct / issue / waiting" without scrolling, a
 * waiting or verified finding must never be counted as a problem, and the free tier must not leak the
 * evidence behind a locked issue.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayAuditViewModelTest {
    private val tpta =
        Anomaly(
            type = "TPTA_ENTITLEMENT",
            field = "transportAllowance",
            amount = 5508.0,
            month = "10/2026",
            description = "TPTA is short.",
            expected = 11268.0,
            actual = 5760.0,
            authority = "GoI MoD letter",
        )
    private val pending = tpta.copy(field = "arrearsDa", amount = 9870.0, isPending = true, month = "04/2026")
    private val verified =
        Anomaly(
            type = "ARREARS_AUDIT",
            field = "arrearsDa",
            amount = 9870.0,
            month = "05/2026",
            description = "Verified: match exactly.",
            expected = 9870.0,
            actual = 9870.0,
        )

    private var calls = mutableListOf<Triple<Int, Int?, Int>>()

    private fun vm(byMonth: Map<Int, List<Anomaly>> = emptyMap()) =
        PayAuditViewModel(
            engine = { current, previous, history ->
                calls += Triple(current.monthNum, previous?.monthNum, history.size)
                EngineResult(healthScore = 0, anomalies = byMonth[current.year * 100 + current.monthNum].orEmpty(), monthlySavingRate = 0.0, taxRatio = 0.0)
            },
            dispatcher = UnconfinedTestDispatcher(),
        )

    private fun slip(
        year: Int,
        month: Int,
    ) =
        ParsedPayslip(
            file = "p.pdf", year = year, monthNum = month, monthName = "M", dateStr = "$month/$year",
            officer = Officer("N", "A", "P"),
            earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
            deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(100.0, 80.0, 20.0),
            taxAndSavings = TaxAndSavings(1000.0, 900.0, 50.0, 850.0, 100.0, 80.0, 20.0, DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0)),
        )

    private val slips = listOf(slip(2026, 3), slip(2026, 4), slip(2026, 5), slip(2026, 10))

    @Test
    fun noPayslipsMeansNoPayslipVerdict() {
        val v = vm()
        v.setInputs(emptyList(), hasAccess = true)
        assertEquals(PayAuditVerdict.NoPayslip, v.uiState.value.verdict)
        assertNull(v.uiState.value.selectedMonth)
    }

    @Test
    fun opensOnLatestMonthAndOffersOnlyMonthsThatHaveAPayslip() {
        val v = vm()
        v.setInputs(slips.reversed(), hasAccess = true)
        assertEquals(PayMonth(2026, 10), v.uiState.value.selectedMonth)
        assertEquals(slips.map { PayMonth(it.year, it.monthNum) }, v.uiState.value.availableMonths)
    }

    @Test
    fun requestedMonthWinsAndUnknownRequestFallsBackToLatest() {
        val v = vm()
        v.setInputs(slips, hasAccess = true, requestedMonth = PayMonth(2026, 4))
        assertEquals(PayMonth(2026, 4), v.uiState.value.selectedMonth)
        v.setInputs(slips, hasAccess = true, requestedMonth = PayMonth(2026, 6))
        assertEquals(PayMonth(2026, 10), v.uiState.value.selectedMonth)
    }

    @Test
    fun cleanMonthSaysAllCorrectWithTheNumberOfLinesChecked() {
        val v = vm()
        v.setInputs(slips, hasAccess = true)
        val verdict = v.uiState.value.verdict as PayAuditVerdict.Clean
        assertEquals(getCreditsList(slips.last()).size + getDebitsList(slips.last()).size, verdict.linesChecked)
        assertTrue(verdict.linesChecked > 0)
        assertEquals(0, verdict.verifiedCount)
    }

    @Test
    fun provenIssueNamesThePayLineAndTheShortfall() {
        val v = vm(mapOf(202610 to listOf(tpta)))
        v.setInputs(slips, hasAccess = true)
        assertEquals(PayAuditVerdict.Issue(1, listOf("TPTA (transport allowance)"), 5508.0, canDraftLetter = true), v.uiState.value.verdict)
        assertEquals(listOf(tpta), v.uiState.value.findings.issues)
    }

    @Test
    fun lockedIssueKeepsCountAndPayLineButLeaksNoEvidence() {
        val v = vm(mapOf(202610 to listOf(tpta)))
        v.setInputs(slips, hasAccess = false)
        assertEquals(PayAuditVerdict.LockedIssue(1, listOf("TPTA (transport allowance)")), v.uiState.value.verdict)
        assertEquals(PayAuditMonthFindings(), v.uiState.value.findings)
        assertTrue(v.uiState.value.isLocked)
    }

    @Test
    fun pendingFindingIsWaitingNotAnIssue() {
        val v = vm(mapOf(202604 to listOf(pending)))
        v.setInputs(slips, hasAccess = true, requestedMonth = PayMonth(2026, 4))
        assertEquals(PayAuditVerdict.Waiting(1), v.uiState.value.verdict)
        assertEquals(listOf(pending), v.uiState.value.findings.waiting)
        assertTrue(v.uiState.value.findings.issues.isEmpty())
    }

    @Test
    fun verifiedArrearsStayVisibleButCountAsCorrect() {
        val v = vm(mapOf(202605 to listOf(verified, verified.copy(description = "second"))))
        v.setInputs(slips, hasAccess = true, requestedMonth = PayMonth(2026, 5))
        val verdict = v.uiState.value.verdict as PayAuditVerdict.Clean
        assertEquals(2, verdict.verifiedCount)
        assertEquals(2, v.uiState.value.findings.verified.size)
        assertTrue(v.uiState.value.findings.issues.isEmpty())
    }

    @Test
    fun arrearsShortfallIsAnIssueSoTheVerdictNeverSaysCleanOverIt() {
        // DaArrearsAuditor reports an under-paid arrears as SALARY_LOSS (expected/actual set, no authority).
        val short = Anomaly("SALARY_LOSS", "arrearsDa", 2000.0, "10/2026", "Underpaid/Mismatched", expected = 9870.0, actual = 7870.0)
        val v = vm(mapOf(202610 to listOf(short)))
        v.setInputs(slips, hasAccess = true)
        assertEquals(PayAuditVerdict.Issue(1, listOf("DA arrears"), 2000.0, canDraftLetter = false), v.uiState.value.verdict)
    }

    @Test
    fun aBareSalaryLossHeuristicIsStillNotAnIssue() {
        val bare = Anomaly("SALARY_LOSS", "netPay", 500.0, "10/2026", "Net pay dropped")
        val v = vm(mapOf(202610 to listOf(bare)))
        v.setInputs(slips, hasAccess = true)
        assertTrue(v.uiState.value.verdict is PayAuditVerdict.Clean)
    }

    @Test
    fun anomaliesFromOtherAuditorsNeverReachThePayAuditVerdict() {
        // Carried over from the old PayAuditFindingsLogicTest: DSOP/tax findings belong to other screens.
        val other = tpta.copy(type = "DSOP_COMPLIANCE")
        val v = vm(mapOf(202610 to listOf(other, other.copy(type = "TAX_PROJECTION"))))
        v.setInputs(slips, hasAccess = true)
        assertTrue(v.uiState.value.verdict is PayAuditVerdict.Clean)
        assertTrue(v.uiState.value.findings.issues.isEmpty())
    }

    @Test
    fun issueOutranksWaitingInTheVerdict() {
        val v = vm(mapOf(202610 to listOf(pending, tpta)))
        v.setInputs(slips, hasAccess = true)
        assertTrue(v.uiState.value.verdict is PayAuditVerdict.Issue)
    }

    @Test
    fun historySummaryCountsIssuesButNotWaitingOrVerified() {
        val v = vm(mapOf(202610 to listOf(tpta), 202604 to listOf(pending), 202605 to listOf(verified)))
        v.setInputs(slips, hasAccess = true)
        assertEquals(PayAuditHistorySummary(monthsAudited = 4, issues = 1), v.uiState.value.history)
    }

    @Test
    fun everyMonthIsAnalysedAgainstItsPredecessorAndOnlyOnce() {
        val v = vm(mapOf(202610 to listOf(tpta)))
        v.setInputs(slips, hasAccess = true)
        v.selectMonth(PayMonth(2026, 5))
        v.selectMonth(PayMonth(2026, 10))
        assertEquals(PayMonth(2026, 10), v.uiState.value.selectedMonth)
        assertTrue(calls.contains(Triple(5, 4, 4)), "May must be analysed against April with the full history: $calls")
        assertEquals(1, calls.count { it.first == 5 }, "switching back and forth must reuse the cached analysis: $calls")
    }

    @Test
    fun switchingMonthChangesTheVerdictToThatMonthsFindings() {
        val v = vm(mapOf(202610 to listOf(tpta)))
        v.setInputs(slips, hasAccess = true)
        assertTrue(v.uiState.value.verdict is PayAuditVerdict.Issue)
        v.selectMonth(PayMonth(2026, 5))
        assertTrue(v.uiState.value.verdict is PayAuditVerdict.Clean)
    }

    @Test
    fun selectingAMonthWithoutAPayslipIsIgnored() {
        val v = vm()
        v.setInputs(slips, hasAccess = true)
        v.selectMonth(PayMonth(2026, 6))
        assertEquals(PayMonth(2026, 10), v.uiState.value.selectedMonth)
    }

    @Test
    fun tabSelectionSurvivesMonthSwitchAndInputRefresh() {
        val v = vm()
        v.setInputs(slips, hasAccess = true)
        v.selectTab(PayAuditTab.HISTORY)
        v.selectMonth(PayMonth(2026, 4))
        v.setInputs(slips, hasAccess = true, requestedMonth = null)
        assertEquals(PayAuditTab.HISTORY, v.uiState.value.tab)
        assertEquals(PayMonth(2026, 4), v.uiState.value.selectedMonth)
    }

    @Test
    fun unlockingRevealsTheEvidenceWithoutReselectingTheMonth() {
        val v = vm(mapOf(202610 to listOf(tpta)))
        v.setInputs(slips, hasAccess = false)
        v.setInputs(slips, hasAccess = true)
        assertTrue(v.uiState.value.verdict is PayAuditVerdict.Issue)
    }
}
