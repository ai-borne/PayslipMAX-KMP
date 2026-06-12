package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip

data class FinancialInsight(
    val title: String,
    val description: String,
    val icon: String,
    val type: String,
)

object FinancialInsightsGenerator {
    fun generate(payslip: ParsedPayslip, previousPayslip: ParsedPayslip? = null): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val gross = payslip.summary.grossPay

        if (gross <= 0.0) return insights

        // 1. DSOP Savings Rate Analysis
        val basicPay = payslip.earnings.basicPay
        val dsop = payslip.deductions.dsopSubscription

        if (dsop > 0.0) {
            val dsopRate = (dsop / gross) * 100.0
            insights.add(
                FinancialInsight(
                    title = "Excellent Savings Rate (${dsopRate.toString().take(4)}%)",
                    description = "You saved ₹${formatAmount(
                        dsop,
                    )} in your DSOP Fund this month, which represents ${dsopRate.toString().take(
                        4,
                    )}% of your gross earnings. The compounding tax-free interest makes DSOP an elite wealth aggregator.",
                    icon = "📈",
                    type = "success",
                ),
            )
        } else if (basicPay > 0.0) {
            insights.add(
                FinancialInsight(
                    title = "Action Required: Zero DSOP Contribution",
                    description = "No DSOP subscription was deducted this month. Under Indian Army rules, a minimum contribution of 6% of your Basic Pay is mandatory for retirement security.",
                    icon = "⚠️",
                    type = "warning",
                ),
            )
        }

        // 2. Tax Burden Analysis
        val tax = payslip.deductions.incomeTax + payslip.deductions.educationCess
        if (tax > 0.0) {
            val taxRate = (tax / gross) * 100.0
            insights.add(
                FinancialInsight(
                    title = "Income Tax Burden (${taxRate.toString().take(4)}%)",
                    description = "A total of ₹${formatAmount(
                        tax,
                    )} was deducted for Income Tax & Cess this month, consuming ${taxRate.toString().take(
                        4,
                    )}% of your gross salary. Review tax projections for annual planning.",
                    icon = "🏛️",
                    type = "accent",
                ),
            )
        }

        // 3. Accommodation Subsidy Analysis
        val lf = payslip.deductions.licenseFee
        val fur = payslip.deductions.furnitureRent
        if (lf > 0.0) {
            val totalAcc = lf + fur
            insights.add(
                FinancialInsight(
                    title = "Government Housing Subsidy",
                    description = "You paid a License Fee of ₹${formatAmount(
                        lf,
                    )} and Furniture Rent of ₹${formatAmount(
                        fur,
                    )} (Total: ₹${formatAmount(
                        totalAcc,
                    )}). Occupying government quarters represents an implied subsidy of ₹20,000+ per month compared to market rentals.",
                    icon = "🏠",
                    type = "success",
                ),
            )
        }

        // 4. Special Forces Pay
        val sfPay = payslip.earnings.specialForcesPay
        if (sfPay > 0.0) {
            insights.add(
                FinancialInsight(
                    title = "Special Forces Allowance Active",
                    description = "Your ledger includes ₹${formatAmount(
                        sfPay,
                    )} credited as Special Forces/Command Pay. This is an elite hazard allowance recognizing specialized operational service.",
                    icon = "🦅",
                    type = "success",
                ),
            )
        }

        // 5. Arrears & Adjustments Advice
        val arrearsSum =
            payslip.earnings.arrearsCea +
                payslip.earnings.arrearsDa +
                payslip.earnings.arrearsRation +
                payslip.earnings.arrearsSpecialForces +
                payslip.earnings.arrearsTpta +
                payslip.earnings.arrearsTptaDa

        if (arrearsSum > 0.0) {
            insights.add(
                FinancialInsight(
                    title = "Arrears Credited: ₹${formatAmount(arrearsSum)}",
                    description = "You received lump-sum arrears this month. A standard wealth building strategy is to route these unexpected credits directly into long-term investments rather than general spending.",
                    icon = "💰",
                    type = "success",
                ),
            )
        }

        // 6. Military Service Pay (MSP) Education
        val msp = payslip.earnings.militaryServicePay
        if (msp > 0.0) {
            insights.add(
                FinancialInsight(
                    title = "Military Service Pay (₹${formatAmount(msp)})",
                    description = "MSP is a distinct military allowance paid under the 7th Pay Commission. Unlike civil services, it specifically compensates for the constant relocations, hazards, and severe constraints of active military life.",
                    icon = "🎖️",
                    type = "accent",
                ),
            )
        }

        // 7. Utility Spike Analysis
        if (previousPayslip != null) {
            val currentElec = payslip.deductions.electricityCharges
            val prevElec = previousPayslip.deductions.electricityCharges
            if (prevElec > 0.0 && currentElec > prevElec * 1.2 && (currentElec - prevElec) > 200.0) {
                insights.add(
                    FinancialInsight(
                        title = "Utility Charge Spike Detected",
                        description = "Your electricity charges increased from ₹${formatAmount(prevElec)} to ₹${formatAmount(currentElec)} (+${((currentElec - prevElec) / prevElec * 100).toInt()}%). Consider checking for billing anomalies or seasonal surges.",
                        icon = "⚡",
                        type = "warning"
                    )
                )
            }
        }

        // 8. Missing Active Allowance Audit
        if (previousPayslip != null) {
            val prevSf = previousPayslip.earnings.specialForcesPay
            val currSf = payslip.earnings.specialForcesPay
            if (prevSf > 0.0 && currSf == 0.0) {
                insights.add(
                    FinancialInsight(
                        title = "Allowance Recovery Alert (SF)",
                        description = "Special Forces Pay (₹${formatAmount(prevSf)}) was credited in your last statement but is absent this month. Please audit with your CDA office if this wasn't an expected posting transfer.",
                        icon = "⚠️",
                        type = "warning"
                    )
                )
            }

            val prevField = previousPayslip.earnings.fieldAllowance
            val currField = payslip.earnings.fieldAllowance
            if (prevField > 0.0 && currField == 0.0) {
                insights.add(
                    FinancialInsight(
                        title = "Allowance Recovery Alert (Field)",
                        description = "Field Allowance (₹${formatAmount(prevField)}) was active in your last statement but is absent this month. Please audit with your unit clerk if posting status changed.",
                        icon = "⚠️",
                        type = "warning"
                    )
                )
            }
        }

        return insights
    }

    private fun formatAmount(amount: Double): String {
        val longVal = amount.toLong()
        val str = longVal.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val builder = StringBuilder()
        var i = remaining.length
        while (i > 0) {
            if (i >= 2) {
                builder.insert(0, remaining.substring(i - 2, i))
                if (i - 2 > 0) builder.insert(0, ",")
                i -= 2
            } else {
                builder.insert(0, remaining.substring(0, 1))
                i -= 1
            }
        }
        return "$builder,$lastThree"
    }
}
