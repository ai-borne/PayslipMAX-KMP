package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase

/**
 * The leave bank as the latest statement prints it, plus what it is worth at today's pay. [balanceDays] is PCDA's printed
 * total and may exceed the cap; [encashableDays] is the part that can be encashed at retirement. [asOfDate] (ISO) is the
 * date of the accrual that total belongs to, which is earlier than the statement month ([sourceMonth]/[sourceYear]).
 */
data class LeaveBankSummary(
    val balanceDays: Int,
    val asOfDate: String,
    val sourceMonth: Int,
    val sourceYear: Int,
    val daysOverCap: Int,
    val encashableDays: Int,
    val ltcDaysUsed: Int,
    val ltcCapDays: Int,
    val valueAtCurrentPay: Double,
)

/**
 * Single source of truth for the Leave Bank figures every surface shows (payslip view, Retirement Calculators, Smart
 * Insight). Pure and gate-agnostic.
 *
 * Only the latest statement that prints a leave table is read, whatever its month: statements list different rows from
 * year to year, so years are never merged, and an older table is never used in place of a newer one. Pay comes from the
 * latest payslip overall, which may be a later month than the leave table.
 */
object LeaveBankCalculator {
    /** Chronological, with the file name breaking same-month ties so the result never depends on input order. */
    private val chronological = compareBy<ParsedPayslip>({ it.year }, { it.monthNum }, { it.file })

    /** Two accruals on the same date: the later order is the newer record; a full tie keeps printed order. */
    private val accrualOrder = compareBy<LeaveEntry>({ it.casDate }, { it.orderDate })

    fun summarize(payslips: List<ParsedPayslip>): LeaveBankSummary? {
        val source = payslips.filter { it.leaveAccumulation != null }.maxWithOrNull(chronological) ?: return null
        val entries = source.leaveAccumulation?.entries.orEmpty()
        val latestAccrual = entries.filter { it.casCode == LeaveCasCode.ACCRUED }.sortedWith(accrualOrder).lastOrNull() ?: return null
        val balance = latestAccrual.totalDays ?: return null
        val pay = payslips.maxWith(chronological).earnings
        val encashable = minOf(balance, PensionRuleKnowledgeBase.LEAVE_MAX_DAYS)

        return LeaveBankSummary(
            balanceDays = balance,
            asOfDate = latestAccrual.casDate,
            sourceMonth = source.monthNum,
            sourceYear = source.year,
            daysOverCap = (balance - PensionRuleKnowledgeBase.LEAVE_MAX_DAYS).coerceAtLeast(0),
            encashableDays = encashable,
            ltcDaysUsed = entries.filter { it.casCode == LeaveCasCode.LTC_ENCASHED }.sumOf { it.days },
            ltcCapDays = PensionRuleKnowledgeBase.LTC_ENCASH_MAX_DAYS,
            valueAtCurrentPay =
                RetirementCalculatorEngine.leaveEncashment(pay.basicPay, pay.dearnessAllowance, encashable, pay.nonPracticingAllowance),
        )
    }
}
