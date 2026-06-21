package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip

class MissingAllowanceAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        if (previous == null) return anomalies

        val checks =
            listOf(
                "houseRentAllowance" to "House Rent Allowance (HRA)",
                "transportAllowance" to "Transport Allowance (TPTA)",
                "militaryServicePay" to "Military Service Pay (MSP)",
            )

        for ((field, name) in checks) {
            val prevVal = getAllowanceValue(previous, field)
            val currVal = getAllowanceValue(current, field)
            if (prevVal > 0.0 && currVal == 0.0) {
                anomalies.add(
                    Anomaly(
                        type = "MISSING_ALLOWANCE",
                        field = field,
                        amount = prevVal,
                        month = current.dateStr,
                        description = "Your active $name of ₹${prevVal.toInt()} in the previous month is missing in this statement.",
                    ),
                )
            }
        }

        return anomalies
    }

    private fun getAllowanceValue(
        payslip: ParsedPayslip,
        field: String,
    ): Double {
        return when (field) {
            "houseRentAllowance" -> payslip.earnings.houseRentAllowance
            "transportAllowance" -> payslip.earnings.transportAllowance
            "militaryServicePay" -> payslip.earnings.militaryServicePay
            else -> 0.0
        }
    }
}
