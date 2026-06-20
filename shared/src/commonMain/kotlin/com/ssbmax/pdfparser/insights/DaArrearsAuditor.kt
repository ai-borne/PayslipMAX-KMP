package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip
import kotlin.math.abs

class DaArrearsAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        val arrearsDa = current.earnings.arrearsDa
        val arrearsTptaDa = current.earnings.arrearsTptaDa

        if (arrearsDa <= 0.0 && arrearsTptaDa <= 0.0) {
            return anomalies
        }

        val basic = current.earnings.basicPay
        val msp = current.earnings.militaryServicePay
        val tpta = current.earnings.transportAllowance

        if (basic <= 0.0 || previous == null) {
            return anomalies
        }

        val currentRate = current.earnings.dearnessAllowance / (basic + msp)
        val prevBasic = previous.earnings.basicPay
        val prevMsp = previous.earnings.militaryServicePay
        val prevRate = previous.earnings.dearnessAllowance / (prevBasic + prevMsp)
        val rateDiff = currentRate - prevRate

        val effectiveRateDiff = if (rateDiff <= 0.0) 0.02 else rateDiff

        val expectedDa = (basic + msp) * effectiveRateDiff * 3.0
        checkArrears(anomalies, arrearsDa, expectedDa, current.dateStr, "Dearness Allowance (DA)")
        
        if (tpta > 0.0 && arrearsTptaDa > 0.0) {
            val expectedTptaDa = tpta * effectiveRateDiff * 3.0
            checkArrears(anomalies, arrearsTptaDa, expectedTptaDa, current.dateStr, "Transport Allowance DA (TPTA DA)")
        }

        return anomalies
    }

    private fun checkArrears(
        anomalies: MutableList<Anomaly>,
        actual: Double,
        expected: Double,
        month: String,
        label: String
    ) {
        if (actual <= 0.0) return
        val diff = abs(actual - expected)
        if (diff < 100.0) {
            anomalies.add(
                Anomaly(
                    type = "ARREARS_AUDIT",
                    field = "arrearsDa",
                    amount = actual,
                    month = month,
                    description = "Verified: Your $label arrears of ₹${actual.toInt()} match the expected calculation exactly."
                )
            )
        } else {
            anomalies.add(
                Anomaly(
                    type = "SALARY_LOSS",
                    field = "arrearsDa",
                    amount = expected - actual,
                    month = month,
                    description = "Underpaid/Mismatched: Your $label arrears of ₹${actual.toInt()} do not match the expected calculation of ₹${expected.toInt()}."
                )
            )
        }
    }
}
