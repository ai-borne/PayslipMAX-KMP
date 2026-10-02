package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

class TaxProjectionAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()

        val currentTax = current.deductions.incomeTax
        val previousTax = previous?.deductions?.incomeTax ?: 0.0

        val projectedTax = current.taxAndSavings?.totalTaxPayable ?: (currentTax * 12.0)
        val gross = current.summary.grossPay

        val isApril = current.monthNum == 4
        val isTaxSpike = previousTax > 0.0 && currentTax > previousTax * 1.20 && (currentTax - previousTax) > 1000.0

        if (isApril) {
            val taxRatio = if (gross > 0.0) (projectedTax / (gross * 12.0)) * 100.0 else 0.0
            anomalies.add(
                Anomaly(
                    type = "TAX_PROJECTION",
                    field = "incomeTax",
                    amount = projectedTax,
                    month = current.dateStr,
                    description = "New FY Tax Projection: April starts the new tax cycle. Estimated annual tax liability is ${PayAuditWording.rupees(projectedTax)} (${taxRatio.toString().take(4)}% of gross).",
                ),
            )
        } else if (isTaxSpike) {
            val spikeAmount = currentTax - previousTax
            anomalies.add(
                Anomaly(
                    type = "DEDUCTION_SPIKE",
                    field = "incomeTax",
                    amount = spikeAmount,
                    month = current.dateStr,
                    description =
                        "Income Tax deduction rose by ${PayAuditWording.rupees(spikeAmount)} (+${(((currentTax - previousTax) / previousTax) * 100).toInt()}%) over your previous payslip. " +
                            "Review your tax deductions now so your take-home pay stays steady in Jan/Feb.",
                ),
            )
        }

        return anomalies
    }
}
