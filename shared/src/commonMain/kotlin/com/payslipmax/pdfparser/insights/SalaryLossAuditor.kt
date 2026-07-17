package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

class SalaryLossAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        if (previous == null) return anomalies

        val prevNet = previous.summary.netRemittance
        val currNet = current.summary.netRemittance

        val netLoss = prevNet - currNet
        val threshold = prevNet * 0.01 // 1% threshold

        if (netLoss > threshold && netLoss > 500.0) {
            val basicDiff = previous.earnings.basicPay - current.earnings.basicPay
            val daDiff = previous.earnings.dearnessAllowance - current.earnings.dearnessAllowance
            val hraDiff = previous.earnings.houseRentAllowance - current.earnings.houseRentAllowance
            val taxDiff = current.deductions.incomeTax - previous.deductions.incomeTax

            val reason =
                when {
                    hraDiff > 0.0 -> "primarily due to the absence or reduction of House Rent Allowance (HRA) by ₹${hraDiff.toInt()}"
                    basicDiff > 0.0 -> "due to an adjustment or drop in Basic Pay by ₹${basicDiff.toInt()}"
                    taxDiff > 0.0 -> "due to a spike in Income Tax deductions by ₹${taxDiff.toInt()}"
                    else -> "due to minor fluctuations across multiple pay and deduction elements"
                }

            anomalies.add(
                Anomaly(
                    type = "SALARY_LOSS",
                    field = "netPay",
                    amount = netLoss,
                    month = current.dateStr,
                    description = "Your net salary reduced by ₹${netLoss.toInt()} compared to the previous month, $reason.",
                ),
            )
        }

        return anomalies
    }
}
