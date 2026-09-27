package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissingAllowanceAuditorTest {
    private val auditor = MissingAllowanceAuditor()

    private fun pair(
        hra: Pair<Double, Double> = 0.0 to 0.0,
        msp: Pair<Double, Double> = 15500.0 to 15500.0,
        licenseFee: Double = 0.0,
        basic: Double = 85300.0,
    ) = listOf(
        payAuditPayslip(2018, 1, basic, hra = hra.first, msp = msp.first),
        payAuditPayslip(2018, 2, basic, hra = hra.second, msp = msp.second, licenseFee = licenseFee),
    )

    private fun run(months: List<ParsedPayslip>) = auditor.audit(months[1], months[0], months.take(1))

    @Test
    fun droppedHraIsFlaggedWithBothAmounts() {
        val finding = run(pair(hra = 27000.0 to 0.0)).single()
        assertEquals("houseRentAllowance", finding.field)
        assertEquals(27000.0, finding.expected)
        assertEquals(0.0, finding.actual)
    }

    @Test
    fun hraStoppingWhenGovernmentQuartersAreTakenIsExplained() {
        assertTrue(run(pair(hra = 27000.0 to 0.0, licenseFee = 748.0)).isEmpty())
    }

    @Test
    fun droppedMspIsFlagged() {
        assertEquals("militaryServicePay", run(pair(msp = 15500.0 to 0.0)).single().field)
    }

    @Test
    fun mspStoppingAtLevelFourteenIsExplained() {
        assertTrue(run(pair(msp = 15500.0 to 0.0, basic = 144200.0)).isEmpty())
    }

    @Test
    fun droppedTptaIsLeftToTheTptaAuditorSoItIsNotReportedTwice() {
        val months = listOf(payAuditPayslip(2018, 1, 85300.0), payAuditPayslip(2018, 2, 85300.0, tpta = 0.0))
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun anEmptyParsedMonthIsNotReportedAsEverythingMissing() {
        val months = listOf(payAuditPayslip(2018, 1, 85300.0, hra = 27000.0), payAuditPayslip(2018, 2, 0.0, msp = 0.0, tpta = 0.0))
        assertTrue(run(months).isEmpty())
    }
}
