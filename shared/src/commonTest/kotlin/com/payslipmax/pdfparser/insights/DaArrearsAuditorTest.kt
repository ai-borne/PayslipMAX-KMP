package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
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
        assertEquals("Verified: your Dearness Allowance (DA) arrears of ₹10,395 match the amount worked out from your DA rise exactly.", anomaly.description)
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
        assertEquals(
            "Your Dearness Allowance (DA) arrears are ₹5,000, which is ₹5,395 less than the ₹10,395 worked out from your DA rise. " +
                "If part of it is paid on a later payslip, this will clear.",
            anomaly.description,
        )
    }

    // A DA rise effective 1 Jan and first paid in March covers only Jan + Feb. Assuming a fixed 3 months
    // reported a correct payment as "underpaid" and offered a complaint letter (real corpus case, Mar 2024).
    @Test
    fun twoMonthArrearsForJanuaryRisePaidInMarchIsVerified() {
        // (140500 + 15500) * 4% * 2 months = 12480
        // TPTA-DA arrears: 3600 * 4% * 2 months = 288
        val previous = createMockPayslip("02/2024", basicPay = 140500.0, da = 71760.0, tpta = 3600.0, tptada = 1656.0, monthNum = 2, year = 2024)
        val current =
            createMockPayslip(
                "03/2024",
                basicPay = 140500.0,
                da = 78000.0,
                tpta = 3600.0,
                tptada = 1800.0,
                arrearsDa = 12480.0,
                arrearsTptaDa = 288.0,
                monthNum = 3,
                year = 2024,
            )

        val result = DaArrearsAuditor().audit(current, previous, emptyList())

        assertTrue(result.none { it.type == "SALARY_LOSS" }, "correct 2-month arrears must not be flagged: $result")
        assertTrue(result.any { it.type == "ARREARS_AUDIT" })
    }

    // Both arrears checks used field "arrearsDa", so two cards on the Pay Audit screen carried the same title
    // ("DA arrears") and the user could not tell which pay line each one verified.
    @Test
    fun tptaDaArrearsAreReportedUnderTheirOwnPayLine() {
        val previous = createMockPayslip("02/2024", basicPay = 140500.0, da = 71760.0, tpta = 3600.0, tptada = 1656.0, monthNum = 2, year = 2024)
        val current =
            createMockPayslip(
                "03/2024",
                basicPay = 140500.0,
                da = 78000.0,
                tpta = 3600.0,
                tptada = 1800.0,
                arrearsDa = 12480.0,
                arrearsTptaDa = 288.0,
                monthNum = 3,
                year = 2024,
            )

        val fields = DaArrearsAuditor().audit(current, previous, emptyList()).filter { it.type == "ARREARS_AUDIT" }.map { it.field }

        assertEquals(listOf("arrearsDa", "arrearsTptaDa"), fields)
    }

    // Arrears with no DA rise are some other payment; inventing a 2% rise produced fake mismatches.
    @Test
    fun arrearsWithoutRateRiseAreNotAudited() {
        val previous = createMockPayslip("03/2026", da = 61215.0)
        val current = createMockPayslip("04/2026", da = 61215.0, arrearsDa = 5000.0)

        assertTrue(DaArrearsAuditor().audit(current, previous, emptyList()).isEmpty())
    }

    // Pre-2024 payslips print TPTA inclusive of its DA and fold TPTA-DA arrears into the DA arrears line
    // (real corpus case, Apr 2018: 6048 DA + 216 TPTA-DA = 6264). That total is correct, not a mismatch.
    @Test
    fun mergedTptaDaArrearsOnCombinedTptaPayslipIsVerified() {
        // DA 5% -> 7% on (85300 + 15500); TPTA 3600 base printed as 3852 (3600 * 1.07).
        val previous = createMockPayslip("03/2018", basicPay = 85300.0, da = 5040.0, tpta = 3780.0, tptada = 0.0, monthNum = 3, year = 2018)
        val current = createMockPayslip("04/2018", basicPay = 85300.0, da = 7056.0, tpta = 3852.0, tptada = 0.0, arrearsDa = 6264.0, monthNum = 4, year = 2018)

        val result = DaArrearsAuditor().audit(current, previous, emptyList())

        assertTrue(result.none { it.type == "SALARY_LOSS" }, "DA + merged TPTA-DA arrears must not be flagged: $result")
        assertTrue(result.any { it.type == "ARREARS_AUDIT" })
    }

    // Paying more than the formula is not a salary loss, so it must never drive a complaint letter.
    @Test
    fun overpaidArrearsAreNotReportedAsSalaryLoss() {
        val previous = createMockPayslip("03/2026", da = 57750.0)
        val current = createMockPayslip("04/2026", da = 61215.0, arrearsDa = 20000.0)

        assertTrue(DaArrearsAuditor().audit(current, previous, emptyList()).none { it.type == "SALARY_LOSS" })
    }

    // 6th CPC-era DA (100%+ on a different pay base) does not follow 7th CPC arrears math.
    @Test
    fun sixthCpcEraPayslipIsNotAudited() {
        val previous = createMockPayslip("12/2016", basicPay = 31590.0, msp = 6000.0, da = 46988.0, monthNum = 12, year = 2016)
        val current = createMockPayslip("01/2017", basicPay = 31590.0, msp = 6000.0, da = 49619.0, arrearsDa = 16458.0, monthNum = 1, year = 2017)

        assertTrue(DaArrearsAuditor().audit(current, previous, emptyList()).isEmpty())
    }
}
