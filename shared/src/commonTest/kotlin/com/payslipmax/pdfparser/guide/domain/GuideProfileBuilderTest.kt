package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The profile must come from Pay Audit's own reading of the payslips (the service timeline), not a second parser:
 * the Guide and Pay Audit can then never disagree about the officer's level, DA or Transport Allowance class.
 */
class GuideProfileBuilderTest {
    private fun payslip(
        year: Int,
        month: Int,
        basic: Double,
        daPercent: Double = 60.0,
        msp: Double = 15500.0,
        tpta: Double = 0.0,
        tptaDa: Double = 0.0,
        hra: Double = 0.0,
        needsReview: Boolean = false,
    ) = ParsedPayslip(
        file = "t.pdf",
        year = year,
        monthNum = month,
        monthName = "",
        dateStr = "$month/$year",
        officer = Officer("N", "A", "P"),
        earnings =
            Earnings(
                basicPay = basic,
                militaryServicePay = msp,
                dearnessAllowance = ((basic + msp) * daPercent / 100.0).roundToInt().toDouble(),
                transportAllowance = tpta,
                transportAllowanceDa = tptaDa,
                houseRentAllowance = hra,
            ),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
        needsReview = needsReview,
    )

    @Test
    fun theProfileIsTheLatestMonthWithItsLevelBasicDaAndHra() {
        val profile =
            GuideProfileBuilder.from(
                listOf(payslip(2026, 4, 121200.0, hra = 36360.0), payslip(2026, 5, 121200.0, hra = 36360.0, tpta = 7200.0, tptaDa = 4320.0)),
            )!!

        assertEquals(2026 to 5, profile.year to profile.month)
        assertEquals("12A", profile.level)
        assertEquals(121200.0, profile.basicPay)
        assertEquals(60, profile.daPercent)
        assertEquals("HIGHER", profile.tptaClass)
        assertEquals(36360.0, profile.hraAmount)
    }

    @Test
    fun aLatestPayslipPayAuditExcludesIsNotUsed() {
        val profile = GuideProfileBuilder.from(listOf(payslip(2026, 4, 121200.0), payslip(2026, 5, 121200.0, needsReview = true)))!!

        assertEquals(4, profile.month, "a payslip flagged for review is not a reliable basis, so the earlier month is used and named")
    }

    @Test
    fun anUnreadableDaOrClassStaysNullInsteadOfBeingGuessed() {
        val profile = GuideProfileBuilder.from(listOf(payslip(2026, 5, 121200.0, daPercent = 60.37)))!!

        assertNull(profile.daPercent, "DA that is not a whole percent (arrears folded in) is unknown")
        assertNull(profile.tptaClass)
    }

    @Test
    fun noUsablePayslipMeansNoProfile() {
        assertNull(GuideProfileBuilder.from(emptyList()))
        assertNull(GuideProfileBuilder.from(listOf(payslip(2026, 5, 121200.0, needsReview = true))))
    }
}
