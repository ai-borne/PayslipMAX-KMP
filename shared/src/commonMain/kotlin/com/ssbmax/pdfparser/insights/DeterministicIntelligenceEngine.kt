package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import kotlinx.serialization.Serializable

@Serializable
data class Anomaly(
    val type: String,        // "SALARY_LOSS", "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE", "DSOP_COMPLIANCE"
    val field: String,       // "netPay", "hra", "tpta", "dsop", etc.
    val amount: Double,
    val month: String,       // "MM/YYYY"
    val description: String
)

@Serializable
data class EngineResult(
    val healthScore: Int,
    val anomalies: List<Anomaly>,
    val monthlySavingRate: Double,
    val taxRatio: Double
)

object DeterministicIntelligenceEngine {

    fun analyze(
        current: LedgerRecordEntity,
        previous: LedgerRecordEntity? = null,
        history: List<LedgerRecordEntity> = emptyList()
    ): EngineResult {
        val anomalies = mutableListOf<Anomaly>()

        // 1. Salary Loss Detection
        if (previous != null) {
            val netLoss = previous.netPay - current.netPay
            val threshold = previous.netPay * 0.01 // 1% threshold
            if (netLoss > threshold && netLoss > 500.0) {
                val basicDiff = previous.basicPay - current.basicPay
                val daDiff = previous.dearnessAllowance - current.dearnessAllowance
                val hraDiff = previous.houseRentAllowance - current.houseRentAllowance
                val taxDiff = current.incomeTax - previous.incomeTax

                val reason = when {
                    hraDiff > 0.0 -> "primarily due to the absence or reduction of House Rent Allowance (HRA) by ₹${formatValue(hraDiff)}"
                    basicDiff > 0.0 -> "due to an adjustment or drop in Basic Pay by ₹${formatValue(basicDiff)}"
                    taxDiff > 0.0 -> "due to a spike in Income Tax deductions by ₹${formatValue(taxDiff)}"
                    else -> "due to minor fluctuations across multiple pay and deduction elements"
                }

                anomalies.add(
                    Anomaly(
                        type = "SALARY_LOSS",
                        field = "netPay",
                        amount = netLoss,
                        month = current.dateStr,
                        description = "Your net salary reduced by ₹${formatValue(netLoss)} compared to the previous month, $reason."
                    )
                )
            }
        }

        // 2. Missing Allowance Detection
        if (previous != null) {
            val checkAllowances = listOf(
                "houseRentAllowance" to "House Rent Allowance (HRA)",
                "transportAllowance" to "Transport Allowance (TPTA)",
                "militaryServicePay" to "Military Service Pay (MSP)"
            )
            for ((field, name) in checkAllowances) {
                val prevVal = getFieldValue(previous, field)
                val currVal = getFieldValue(current, field)
                if (prevVal > 0.0 && currVal == 0.0) {
                    anomalies.add(
                        Anomaly(
                            type = "MISSING_ALLOWANCE",
                            field = field,
                            amount = prevVal,
                            month = current.dateStr,
                            description = "Your active $name of ₹${formatValue(prevVal)} in the previous month is missing in this statement."
                        )
                    )
                }
            }
        }

        // 3. TPTA Entitlement Auditor
        if (current.basicPay >= 56100.0 && current.transportAllowance == 0.0) {
            anomalies.add(
                Anomaly(
                    type = "TPTA_ENTITLEMENT",
                    field = "transportAllowance",
                    amount = 3600.0,
                    month = current.dateStr,
                    description = "Basic Pay is ₹${formatValue(current.basicPay)} (Level 10+), but Transport Allowance (TPTA) is missing from your earnings ledger."
                )
            )
        }

        // 4. Deduction Spike Detection
        if (previous != null) {
            val prevTax = previous.incomeTax
            val currTax = current.incomeTax
            if (prevTax > 0.0 && currTax > prevTax * 1.15 && (currTax - prevTax) > 1000.0) {
                anomalies.add(
                    Anomaly(
                        type = "DEDUCTION_SPIKE",
                        field = "incomeTax",
                        amount = currTax - prevTax,
                        month = current.dateStr,
                        description = "Income Tax deduction spiked by ₹${formatValue(currTax - prevTax)} (+${(((currTax - prevTax) / prevTax) * 100).toInt()}%)."
                    )
                )
            }
        }

        // 5. DSOP Retirement Contribution Compliance Check
        val minDsop = current.basicPay * 0.06
        if (current.dsopSubscription == 0.0) {
            anomalies.add(
                Anomaly(
                    type = "DSOP_COMPLIANCE",
                    field = "dsopSubscription",
                    amount = minDsop,
                    month = current.dateStr,
                    description = "DSOP subscription is zero. A minimum contribution of 6% of your Basic Pay (₹${formatValue(minDsop)}) is mandatory."
                )
            )
        } else if (current.dsopSubscription < minDsop) {
            anomalies.add(
                Anomaly(
                    type = "DSOP_COMPLIANCE",
                    field = "dsopSubscription",
                    amount = minDsop - current.dsopSubscription,
                    month = current.dateStr,
                    description = "DSOP contribution ₹${formatValue(current.dsopSubscription)} is below the mandatory 6% threshold (₹${formatValue(minDsop)})."
                )
            )
        } else if (history.size >= 18) {
            val last18 = history.takeLast(18)
            val firstVal = last18.firstOrNull()?.dsopSubscription ?: 0.0
            val unchanged = last18.all { it.dsopSubscription == firstVal }
            val savingRate = (current.dsopSubscription / current.grossPay) * 100.0
            if (unchanged && savingRate < 15.0) {
                anomalies.add(
                    Anomaly(
                        type = "DSOP_COMPLIANCE",
                        field = "dsopSubscription",
                        amount = 0.0,
                        month = current.dateStr,
                        description = "DSOP contribution unchanged for 18+ months at ${savingRate.toInt()}%. Suggest increasing contribution for tax-free compounding retirement growth."
                    )
                )
            }
        }

        val savingRate = if (current.grossPay > 0.0) (current.dsopSubscription / current.grossPay) * 100.0 else 0.0
        val taxRate = if (current.grossPay > 0.0) (current.incomeTax / current.grossPay) * 100.0 else 0.0

        val score = calculateHealthScore(current, previous, anomalies, savingRate)

        return EngineResult(
            healthScore = score,
            anomalies = anomalies,
            monthlySavingRate = savingRate,
            taxRatio = taxRate
        )
    }

    private fun calculateHealthScore(
        current: LedgerRecordEntity,
        previous: LedgerRecordEntity?,
        anomalies: List<Anomaly>,
        savingRate: Double
    ): Int {
        var score = 100

        for (anomaly in anomalies) {
            when (anomaly.type) {
                "MISSING_ALLOWANCE" -> score -= 15
                "SALARY_LOSS" -> score -= 20
                "DEDUCTION_SPIKE" -> score -= 10
                "TPTA_ENTITLEMENT" -> score -= 10
                "DSOP_COMPLIANCE" -> {
                    if (anomaly.amount > 0.0) {
                        score -= 25
                    } else {
                        score -= 5
                    }
                }
            }
        }

        if (savingRate >= 20.0) {
            score += 15
        } else if (savingRate >= 10.0) {
            score += 5
        }

        if (anomalies.isEmpty() && savingRate >= 10.0) {
            score += 10
        }

        return score.coerceIn(0, 100)
    }

    private fun getFieldValue(record: LedgerRecordEntity, fieldName: String): Double {
        return when (fieldName) {
            "basicPay" -> record.basicPay
            "dearnessAllowance" -> record.dearnessAllowance
            "militaryServicePay" -> record.militaryServicePay
            "transportAllowance" -> record.transportAllowance
            "transportAllowanceDa" -> record.transportAllowanceDa
            "houseRentAllowance" -> record.houseRentAllowance
            "dsopSubscription" -> record.dsopSubscription
            "incomeTax" -> record.incomeTax
            else -> 0.0
        }
    }

    private fun formatValue(value: Double): String {
        return value.toInt().toString()
    }
}
