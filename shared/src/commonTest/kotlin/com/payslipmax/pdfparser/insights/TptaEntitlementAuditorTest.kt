package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TptaEntitlementAuditorTest {
    private val auditor = TptaEntitlementAuditor()

    private fun run(history: List<ParsedPayslip>) = auditor.audit(history.last(), history.getOrNull(history.size - 2), history.dropLast(1))

    @Test
    fun missingTptaIsFlaggedWithRateTimesDaAndItsAuthority() {
        val months = payAuditMonths(2018, 1, 3) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = if (m == 3) 0.0 else 3600.0) }
        val finding = run(months).single()
        assertEquals("TPTA_ENTITLEMENT", finding.type)
        assertEquals(3600.0 * 1.17, finding.expected)
        assertEquals(0.0, finding.actual)
        assertEquals(PayAuthorities.TRANSPORT_ALLOWANCE, finding.authority)
    }

    @Test
    fun aMonthNextToAPostingStartIsExplained() {
        // Risk & Hardship starts in April; the March payslip carries no TPTA (officer in transit).
        val months = payAuditMonths(2018, 1, 4) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = if (m == 3) 0.0 else 3600.0, riskHardship = if (m == 4) 20300.0 else 0.0) }
        assertTrue(auditor.audit(months[2], months[1], months).isEmpty())
    }

    @Test
    fun aGapBetweenTwoCityClassesIsExplainedAsARelocation() {
        val months =
            payAuditMonths(2018, 1, 5) { y, m ->
                val tpta =
                    if (m < 3) {
                        3600.0 * 1.17
                    } else if (m == 3) {
                        0.0
                    } else {
                        7200.0 * 1.17
                    }
                payAuditPayslip(y, m, 85300.0, tpta = tpta)
            }
        assertTrue(auditor.audit(months[2], months[1], months).isEmpty())
    }

    @Test
    fun tptaPaidNothingToAudit() {
        val months = payAuditMonths(2018, 1, 3) { y, m -> payAuditPayslip(y, m, 85300.0) }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun beforeSeventhCpcTptaRatesNothingIsFlagged() {
        val months = payAuditMonths(2017, 5, 2) { y, m -> payAuditPayslip(y, m, 82800.0, tpta = 0.0) }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun levelFourteenAndAboveIsNotAuditedBecauseItsSlabDiffers() {
        val months = payAuditMonths(2020, 1, 2) { y, m -> payAuditPayslip(y, m, 144200.0, tpta = 0.0) }
        // 144200 is Level 14 stage 1; Level 13A holds no such cell, so it is unambiguous.
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun anArrearsOrAdjustmentMonthIsNotReportedAsMissing() {
        val months = payAuditMonths(2018, 1, 2) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = 0.0, arrearsTpta = if (m == 2) 500.0 else 0.0) }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun aRelocationGapWithNoFutureSampleYetIsReturnedPendingNotSuppressed() {
        // Same fixture as TptaAbsenceExplainerTest.aRelocationGapWithNoFutureSampleYetIsPending: the
        // higher-city sample that would confirm or rule out a relocation hasn't been imported yet, so the
        // auditor must surface a held finding (P7-17b) instead of returning emptyList().
        val months = payAuditMonths(2018, 1, 3) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = if (m == 3) 0.0 else 3600.0 * 1.17) }
        val finding = run(months).single()
        assertEquals("TPTA_ENTITLEMENT", finding.type)
        assertTrue(finding.isPending)
        assertTrue(finding.expected == null && finding.actual == null && finding.authority == null)
    }
}
