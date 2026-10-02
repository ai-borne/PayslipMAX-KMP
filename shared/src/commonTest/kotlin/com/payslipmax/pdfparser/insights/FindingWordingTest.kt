package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Plain-words wording of the Pay Audit finding descriptions (shown in the Insights tab and on the Pay Audit
 * screen). Why exact strings: these sentences are what an officer reads and may quote to the PCDA(O), so a
 * regression to raw rupees ("₹123100"), "1/2026" months or jargon is a user-visible defect.
 */
class FindingWordingTest {
    private val msp = MspAuditor()
    private val increment = IncrementAuditor()
    private val missing = MissingAllowanceAuditor()
    private val tpta = TptaEntitlementAuditor()

    @Test
    fun partPaidMspNamesWhatIsPaidAndWhatIsDue() {
        val current = payAuditPayslip(2018, 2, 85300.0, msp = 10500.0)
        val finding = msp.audit(current, null, listOf(payAuditPayslip(2018, 1, 85300.0))).single()
        assertEquals("Military Service Pay (MSP) is ₹10,500, but ₹15,500 a month is due at Level 11.", finding.description)
    }

    @Test
    fun aMissedIncrementNamesTheMonthAndGroupsTheAmounts() {
        val l11 = { stage: Int -> PayMatrix.payAt(PayLevel.L11, stage)!!.toDouble() }
        val months = payAuditMonths(2018, 1, 19) { y, m -> payAuditPayslip(y, m, l11(if (y == 2018 && m < 7) 7 else 8)) }
        val finding = increment.audit(months.last(), months[months.size - 2], months.dropLast(1)).single()
        val expected =
            "Your annual increment looks missing. Your last increment took effect in July 2018, so your Basic Pay should rise " +
                "from ${PayAuditWording.rupees(l11(8))} to ${PayAuditWording.rupees(l11(9))} (Level 11). " +
                "Check whether an increment was withheld before raising it."
        assertEquals(expected, finding.description)
        assertFalse(Regex("₹\\d{4,}").containsMatchIn(finding.description), "rupees must be grouped: ${finding.description}")
    }

    @Test
    fun aDroppedAllowanceSaysItWasOnThePreviousPayslipNotThatItIsLost() {
        val months = listOf(payAuditPayslip(2018, 1, 85300.0, hra = 27000.0), payAuditPayslip(2018, 2, 85300.0))
        val finding = missing.audit(months[1], months[0], months.take(1)).single()
        assertEquals("House Rent Allowance (HRA) of ₹27,000 was on your previous payslip but is not on this one.", finding.description)
    }

    @Test
    fun missingTptaStatesTheDueAmountAndHowItIsWorkedOut() {
        val months = payAuditMonths(2018, 1, 3) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = if (m == 3) 0.0 else 3600.0) }
        val finding = tpta.audit(months.last(), months[1], months.dropLast(1)).single()
        assertEquals(
            "Transport Allowance (TPTA) is not on this payslip. At Level 11, at least ₹4,212 a month is due " +
                "(₹3,600 base rate plus 17% Dearness Allowance on it).",
            finding.description,
        )
    }

    @Test
    fun aHeldTptaFindingSaysItIsOnHoldNotThatMoneyIsLost() {
        // Same fixture as TptaEntitlementAuditorTest.aRelocationGapWithNoFutureSampleYetIsReturnedPendingNotSuppressed.
        val months = payAuditMonths(2018, 1, 3) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = if (m == 3) 0.0 else 3600.0 * 1.17) }
        val held = tpta.audit(months.last(), months[1], months.dropLast(1)).single()
        assertEquals(
            "Transport Allowance (TPTA) is not on this payslip. It may be explained by a posting change or relocation, " +
                "so it is on hold until a later payslip confirms.",
            held.description,
        )
    }

    @Test
    fun aNetPayDropFromTaxIsNotDescribedAsALoss() {
        val prev = payAuditPayslip(2018, 1, 85300.0).copy(summary = PayslipSummary(0.0, 0.0, 100000.0), deductions = Deductions(incomeTax = 5000.0))
        val curr = payAuditPayslip(2018, 2, 85300.0).copy(summary = PayslipSummary(0.0, 0.0, 95000.0), deductions = Deductions(incomeTax = 10000.0))
        val finding = SalaryLossAuditor().audit(curr, prev, listOf(prev)).single()
        assertEquals("Your net pay is ₹5,000 lower than on your previous payslip, because ₹5,000 more Income Tax was deducted.", finding.description)
    }
}
