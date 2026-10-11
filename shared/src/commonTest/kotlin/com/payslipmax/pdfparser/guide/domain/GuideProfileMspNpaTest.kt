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
 * The pre-claim worksheet pre-fills MSP and NPA (fields the portal asks for) from the same payslip month as level, basic and
 * DA, so the worksheet and the Guide can never disagree about which month they read. Nothing is stored: the profile is rebuilt
 * from the payslips every time.
 */
class GuideProfileMspNpaTest {
    private fun payslip(
        month: Int,
        msp: Double,
        npa: Double,
        needsReview: Boolean = false,
    ) = ParsedPayslip(
        file = "t.pdf",
        year = 2026,
        monthNum = month,
        monthName = "",
        dateStr = "$month/2026",
        officer = Officer("N", "A", "P"),
        earnings =
            Earnings(
                basicPay = 121200.0,
                militaryServicePay = msp,
                nonPracticingAllowance = npa,
                dearnessAllowance = ((121200.0 + msp) * 0.6).roundToInt().toDouble(),
            ),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
        needsReview = needsReview,
    )

    @Test
    fun mspAndNpaAreReadFromTheSamePayslipMonthAsTheBasic() {
        val profile = GuideProfileBuilder.from(listOf(payslip(4, 15000.0, 9000.0), payslip(5, 15500.0, 10000.0)))!!

        assertEquals(5, profile.month)
        assertEquals(15500.0, profile.mspAmount)
        assertEquals(10000.0, profile.npaAmount)
    }

    @Test
    fun aMonthFlaggedForReviewSuppliesNeitherTheBasicNorTheMspAndNpa() {
        val profile = GuideProfileBuilder.from(listOf(payslip(4, 15000.0, 9000.0), payslip(5, 15500.0, 10000.0, needsReview = true)))!!

        assertEquals(4, profile.month)
        assertEquals(15000.0, profile.mspAmount, "MSP must come from the month the basic came from, not from an unreliable newer one")
        assertEquals(9000.0, profile.npaAmount)
    }

    @Test
    fun aProfileBuiltByHandHasNoMspOrNpaUntilTheyAreGiven() {
        val profile = GuideProfile(2026, 5, "12A", 121200.0, 60, null, null)

        assertNull(profile.mspAmount)
        assertNull(profile.npaAmount)
    }
}
