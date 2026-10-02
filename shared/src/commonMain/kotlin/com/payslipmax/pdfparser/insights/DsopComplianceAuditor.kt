package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

class DsopComplianceAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()

        val basicPay = current.earnings.basicPay
        val grossPay = current.summary.grossPay
        val dsop = current.deductions.dsopSubscription

        if (basicPay <= 0.0) return anomalies

        // 1. Mandatory Minimum Check (6% of Basic Pay)
        val minDsop = basicPay * 0.06
        if (dsop == 0.0) {
            anomalies.add(
                Anomaly(
                    type = "DSOP_COMPLIANCE",
                    field = "dsopSubscription",
                    amount = minDsop,
                    month = current.dateStr,
                    description = "DSOP contribution is zero. The minimum is 6% of your Basic Pay, which is ${PayAuditWording.rupees(minDsop)} a month.",
                ),
            )
        } else if (dsop < minDsop) {
            anomalies.add(
                Anomaly(
                    type = "DSOP_COMPLIANCE",
                    field = "dsopSubscription",
                    amount = minDsop - dsop,
                    month = current.dateStr,
                    description = "Your DSOP contribution of ${PayAuditWording.rupees(dsop)} is below the 6% minimum (${PayAuditWording.rupees(minDsop)}).",
                ),
            )
        }

        // 2. Annual Interest Credit Milestone Check
        val miscAdj = current.taxAndSavings?.dsopFund?.miscAdjYtd ?: 0.0
        val closingBalance = current.taxAndSavings?.dsopFund?.closingBalance ?: 0.0
        if (miscAdj > 0.0 && current.monthNum == 3) {
            anomalies.add(
                Anomaly(
                    type = "DSOP_MILESTONE",
                    field = "dsopSubscription",
                    amount = miscAdj,
                    month = current.dateStr,
                    description =
                        "DSOP Milestone: Tax-free annual interest of ${PayAuditWording.rupees(miscAdj)} was credited to your DSOP fund. " +
                            "Your total DSOP balance is ${PayAuditWording.rupees(closingBalance)}.",
                ),
            )
        }

        // 3. Unchanged subscription warning for 18+ months
        if (history.size >= 18) {
            val last18 = history.takeLast(18)
            val firstVal = last18.firstOrNull()?.deductions?.dsopSubscription ?: 0.0
            val unchanged = last18.all { it.deductions.dsopSubscription == firstVal }
            val savingRate = if (grossPay > 0.0) (dsop / grossPay) * 100.0 else 0.0
            if (unchanged && savingRate < 15.0 && dsop > 0.0) {
                anomalies.add(
                    Anomaly(
                        type = "DSOP_COMPLIANCE",
                        field = "dsopSubscription",
                        amount = 0.0,
                        month = current.dateStr,
                        description = "DSOP contribution unchanged for 18+ months at ${savingRate.toInt()}%. Consider increasing contribution towards ₹41,666/mo (₹5 Lakhs/yr) for 100% tax-free compounding growth under Sec 10(11).",
                    ),
                )
            }
        }

        return anomalies
    }
}
