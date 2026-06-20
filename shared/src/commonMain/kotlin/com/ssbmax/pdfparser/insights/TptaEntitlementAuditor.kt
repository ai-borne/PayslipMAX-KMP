package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip

class TptaEntitlementAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()

        val basicPay = current.earnings.basicPay
        val tpta = current.earnings.transportAllowance

        if (basicPay >= 56100.0 && tpta == 0.0) {
            anomalies.add(
                Anomaly(
                    type = "TPTA_ENTITLEMENT",
                    field = "transportAllowance",
                    amount = 3600.0,
                    month = current.dateStr,
                    description = "Basic Pay is ₹${basicPay.toInt()} (Level 10+), but Transport Allowance (TPTA) is missing from your earnings ledger."
                )
            )
        }

        return anomalies
    }
}
