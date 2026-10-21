package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase

/**
 * Synthetic leave accumulation tables and the payslips that carry them, for the Leave Bank summary, ViewModel and insight
 * tests. No real data: order numbers and pay figures are made up. Balances sit below, at and over the 300-day cap.
 */
object SyntheticLeaveAccumulation {
    const val BELOW_CAP_DAYS = 222
    const val AT_CAP_DAYS = PensionRuleKnowledgeBase.LEAVE_MAX_DAYS
    const val OVER_CAP_DAYS = 301
    const val AS_OF_DATE = "2024-12-31"
    const val BASIC_PAY = 150_000.0
    const val MILITARY_SERVICE_PAY = 15_500.0
    const val DEARNESS_ALLOWANCE = 95_990.0 // 58% of Basic + MSP, as PCDA pays it

    private const val ANNUAL_ACCRUAL_DAYS = 30

    fun accrued(
        casDate: String,
        days: Int,
        totalDays: Int,
        orderNo: String = "101",
        orderDate: String = casDate,
    ) = LeaveEntry(casDate, LeaveCasCode.ACCRUED, days, totalDays, orderNo, orderDate)

    fun ltcEncashed(
        casDate: String,
        days: Int,
        orderNo: String = "202",
        orderDate: String = casDate,
    ) = LeaveEntry(casDate, LeaveCasCode.LTC_ENCASHED, days, null, orderNo, orderDate)

    /** A table whose latest accrual, on [asOfDate], prints [balanceDays]; an earlier accrual and [ltcDays] rows come first. */
    fun table(
        balanceDays: Int,
        asOfDate: String = AS_OF_DATE,
        ltcDays: List<Int> = emptyList(),
    ): LeaveAccumulation {
        val yearBefore = "${asOfDate.take(4).toInt() - 1}${asOfDate.drop(4)}"
        val earlier = accrued(yearBefore, ANNUAL_ACCRUAL_DAYS, (balanceDays - ANNUAL_ACCRUAL_DAYS).coerceAtLeast(0))
        val ltc = ltcDays.map { ltcEncashed(yearBefore, it) }
        return LeaveAccumulation(listOf(earlier) + ltc + accrued(asOfDate, ANNUAL_ACCRUAL_DAYS, balanceDays))
    }

    /** A payslip for [year]/[month] that carries [leave] (null for the eleven months that print no leave page). */
    fun payslip(
        year: Int,
        month: Int,
        leave: LeaveAccumulation? = null,
        basicPay: Double = BASIC_PAY,
        dearnessAllowance: Double = DEARNESS_ALLOWANCE,
        militaryServicePay: Double = MILITARY_SERVICE_PAY,
        npa: Double = 0.0,
        file: String = "synthetic_${year}_$month.pdf",
    ) = ParsedPayslip(
        file = file,
        year = year,
        monthNum = month,
        monthName = "",
        dateStr = "${month.toString().padStart(2, '0')}/$year",
        officer = Officer("N", "A", "P"),
        earnings =
            Earnings(
                basicPay = basicPay,
                militaryServicePay = militaryServicePay,
                dearnessAllowance = dearnessAllowance,
                nonPracticingAllowance = npa,
            ),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
        leaveAccumulation = leave,
    )
}
