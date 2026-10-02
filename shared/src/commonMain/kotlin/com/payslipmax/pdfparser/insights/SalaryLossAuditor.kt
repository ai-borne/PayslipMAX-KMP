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
                    hraDiff > 0.0 -> "because House Rent Allowance (HRA) is ${PayAuditWording.rupees(hraDiff)} lower"
                    basicDiff > 0.0 -> "because Basic Pay is ${PayAuditWording.rupees(basicDiff)} lower"
                    taxDiff > 0.0 -> "because ${PayAuditWording.rupees(taxDiff)} more Income Tax was deducted"
                    else -> "because several pay and deduction items changed a little"
                }

            anomalies.add(
                Anomaly(
                    type = "SALARY_LOSS",
                    field = "netPay",
                    amount = netLoss,
                    month = current.dateStr,
                    description = "Your net pay is ${PayAuditWording.rupees(netLoss)} lower than on your previous payslip, $reason.",
                ),
            )
        }

        return anomalies
    }
}
