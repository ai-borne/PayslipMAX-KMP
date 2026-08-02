package com.payslipmax.pdfparser.insights

import kotlinx.serialization.Serializable

@Serializable
data class TdsRunwayResult(
    val ytdTdsDeducted: Double,
    val parsedMonthCount: Int,
    val remainingMonths: Int,
    val totalAnnualTaxLiability: Double,
    val remainingTaxPayable: Double,
    val currentMonthlyTds: Double,
    val projectedMonthlyTds: Double,
    val hasTdsSpikeWarning: Boolean,
    val spikePercentagePct: Double,
)

object TdsRunwayEngine {
    private const val SPIKE_THRESHOLD_RATIO = 1.30
    private const val MIN_SPIKE_AMOUNT = 1000.0

    fun computeTdsRunway(
        ytdTdsDeducted: Double,
        parsedMonthCount: Int,
        totalAnnualTaxLiability: Double,
        currentMonthlyTds: Double,
    ): TdsRunwayResult {
        val count = maxOf(1, minOf(12, parsedMonthCount))
        val remainingMonths = maxOf(1, 12 - count)
        val remainingTax = maxOf(0.0, totalAnnualTaxLiability - ytdTdsDeducted)
        val projectedMonthlyTds = remainingTax / remainingMonths

        val hasSpike =
            (
                currentMonthlyTds > 0 &&
                    projectedMonthlyTds > (currentMonthlyTds * SPIKE_THRESHOLD_RATIO) &&
                    (projectedMonthlyTds - currentMonthlyTds) >= MIN_SPIKE_AMOUNT
            ) ||
                (currentMonthlyTds == 0.0 && remainingTax >= MIN_SPIKE_AMOUNT && count >= 6)

        val spikePct =
            if (currentMonthlyTds > 0) {
                ((projectedMonthlyTds - currentMonthlyTds) / currentMonthlyTds) * 100.0
            } else if (projectedMonthlyTds > 0) {
                100.0
            } else {
                0.0
            }

        return TdsRunwayResult(
            ytdTdsDeducted = ytdTdsDeducted,
            parsedMonthCount = count,
            remainingMonths = remainingMonths,
            totalAnnualTaxLiability = totalAnnualTaxLiability,
            remainingTaxPayable = remainingTax,
            currentMonthlyTds = currentMonthlyTds,
            projectedMonthlyTds = projectedMonthlyTds,
            hasTdsSpikeWarning = hasSpike,
            spikePercentagePct = spikePct,
        )
    }
}
