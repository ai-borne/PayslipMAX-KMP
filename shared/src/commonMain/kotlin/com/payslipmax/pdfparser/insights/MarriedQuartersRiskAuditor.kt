package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

class MarriedQuartersRiskAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()

        // Rule: Do NOT show if the user draws HRA currently
        if (current.earnings.houseRentAllowance > 0.0) {
            return anomalies
        }

        // Get past months including current, sorted chronologically
        val sortedHistory =
            (history + current)
                .distinctBy { it.dateStr }
                .sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })

        // Check if there are at least 3 months where HRA == 0, License Fee == 0, and Furniture Rent == 0
        var consecutiveMonths = 0
        for (payslip in sortedHistory.reversed()) {
            val hasNoHra = payslip.earnings.houseRentAllowance == 0.0
            val hasNoLicenseFee = payslip.deductions.licenseFee == 0.0
            val hasNoFurnitureRent = payslip.deductions.furnitureRent == 0.0

            if (hasNoHra && hasNoLicenseFee && hasNoFurnitureRent) {
                consecutiveMonths++
            } else {
                break
            }
        }

        if (consecutiveMonths >= 3) {
            val estimatedMonthlyDeduction = 4000.0
            val estimatedTotalRisk = consecutiveMonths * estimatedMonthlyDeduction
            anomalies.add(
                Anomaly(
                    type = "RENT_RECOVERY_RISK",
                    field = "licenseFee",
                    amount = estimatedTotalRisk,
                    month = current.dateStr,
                    description = "Quarters Rent Recovery Risk: You have not received HRA or paid License Fee/Furniture Rent for $consecutiveMonths months. Expected retroactive debt recovery is approximately ₹${estimatedTotalRisk.toInt()}.",
                ),
            )
        }

        return anomalies
    }
}
