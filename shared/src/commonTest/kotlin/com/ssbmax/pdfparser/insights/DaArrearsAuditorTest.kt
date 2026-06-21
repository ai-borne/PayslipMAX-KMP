package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DaArrearsAuditorTest {
    private fun createMockPayslip(
        dateStr: String = "04/2026",
        basicPay: Double = 100000.0,
        da: Double = 50000.0,
        msp: Double = 15500.0,
        tpta: Double = 7200.0,
        tptada: Double = 3600.0,
        hra: Double = 27000.0,
        dsop: Double = 20000.0,
        tax: Double = 15000.0,
        arrearsDa: Double = 0.0,
        arrearsTptaDa: Double = 0.0,
        monthNum: Int = 4,
        year: Int = 2026,
    ): ParsedPayslip {
        val gross = basicPay + da + msp + tpta + tptada + hra + arrearsDa + arrearsTptaDa
        val deductions = dsop + tax
        val net = gross - deductions
        return ParsedPayslip(
            file = "test.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "MonthName",
            dateStr = dateStr,
            officer = Officer("Name", "Acc", "PAN"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = da,
                    militaryServicePay = msp,
                    transportAllowance = tpta,
                    transportAllowanceDa = tptada,
                    houseRentAllowance = hra,
                    arrearsDa = arrearsDa,
                    arrearsTptaDa = arrearsTptaDa,
                ),
            deductions =
                Deductions(
                    dsopSubscription = dsop,
                    incomeTax = tax,
                ),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = gross, totalDeductions = deductions, netRemittance = net),
            taxAndSavings = null,
        )
    }

    @Test
    fun testNoArrearsReturnsEmpty() {
        val current = createMockPayslip()
        val auditor = DaArrearsAuditor()
        val result = auditor.audit(current, null, emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testVerifiedArrearsMatch() {
        // basic=100000, msp=15500 -> total basic + msp = 115500.
        // rate hike from 50% (previous) to 53% (current) -> rateDiff = 0.03.
        // expected da arrears = 115500 * 0.03 * 3 = 10395.
        val previous = createMockPayslip("03/2026", da = 57750.0) // 115500 * 0.50 = 57750
        val current = createMockPayslip("04/2026", da = 61215.0, arrearsDa = 10395.0) // 115500 * 0.53 = 61215

        val auditor = DaArrearsAuditor()
        val result = auditor.audit(current, previous, emptyList())

        assertEquals(1, result.size)
        val anomaly = result.first()
        assertEquals("ARREARS_AUDIT", anomaly.type)
        assertTrue(anomaly.description.contains("Verified"))
    }

    @Test
    fun testMismatchedArrearsDanger() {
        // basic=100000, msp=15500 -> total = 115500.
        // rate hike 0.03 -> expected = 10395.
        // actual arrears = 5000 (underpaid)
        val previous = createMockPayslip("03/2026", da = 57750.0)
        val current = createMockPayslip("04/2026", da = 61215.0, arrearsDa = 5000.0)

        val auditor = DaArrearsAuditor()
        val result = auditor.audit(current, previous, emptyList())

        assertEquals(1, result.size)
        val anomaly = result.first()
        assertEquals("SALARY_LOSS", anomaly.type)
        assertTrue(anomaly.description.contains("Underpaid/Mismatched"))
    }
}
