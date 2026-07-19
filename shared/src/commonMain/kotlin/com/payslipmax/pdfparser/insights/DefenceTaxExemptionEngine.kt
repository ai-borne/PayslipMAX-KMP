package com.payslipmax.pdfparser.insights

import kotlinx.serialization.Serializable

@Serializable
data class Section80CResult(
    val totalClaimed: Double,
    val eligibleDeduction: Double,
    val headroom: Double,
)

@Serializable
data class Section80CCD1BResult(
    val totalClaimed: Double,
    val eligibleDeduction: Double,
    val headroom: Double,
)

@Serializable
data class TaxExemptionBreakdown(
    val sec80C: Section80CResult,
    val sec80CCD1B: Section80CCD1BResult,
    val hraExemption: Double,
    val fieldAllowanceExemption: Double,
    val totalSec10Exemptions: Double,
    val totalOldRegimeDeductions: Double,
)

object DefenceTaxExemptionEngine {
    const val LIMIT_80C = 150_000.0
    const val LIMIT_80CCD1B = 50_000.0

    fun compute80CUsage(
        annualDsop: Double,
        annualAgif: Double,
        other80C: Double = 0.0,
    ): Section80CResult {
        val total = annualDsop + annualAgif + other80C
        val eligible = minOf(LIMIT_80C, total)
        val headroom = maxOf(0.0, LIMIT_80C - total)
        return Section80CResult(
            totalClaimed = total,
            eligibleDeduction = eligible,
            headroom = headroom,
        )
    }

    fun compute80CCD1BUsage(
        currentAnnualNps: Double = 0.0,
    ): Section80CCD1BResult {
        val eligible = minOf(LIMIT_80CCD1B, currentAnnualNps)
        val headroom = maxOf(0.0, LIMIT_80CCD1B - currentAnnualNps)
        return Section80CCD1BResult(
            totalClaimed = currentAnnualNps,
            eligibleDeduction = eligible,
            headroom = headroom,
        )
    }

    fun computeHraExemption(
        annualHraReceived: Double,
        annualRentPaid: Double,
        annualBasicPlusDa: Double,
        isMetro: Boolean = false,
    ): Double {
        if (annualHraReceived <= 0.0 || annualRentPaid <= 0.0) return 0.0
        val rentOver10Pct = maxOf(0.0, annualRentPaid - (0.10 * annualBasicPlusDa))
        val salaryPercentage = (if (isMetro) 0.50 else 0.40) * annualBasicPlusDa
        return minOf(annualHraReceived, rentOver10Pct, salaryPercentage)
    }

    fun extractExemptions(
        summary: FyTaxLedgerSummary,
        rentPaidAnnual: Double = 0.0,
        other80CAnnual: Double = 0.0,
        npsAnnual: Double = 0.0,
        isMetro: Boolean = false,
    ): TaxExemptionBreakdown {
        val sec80C = compute80CUsage(summary.projectedAnnualDsop, summary.projectedAnnualAgif, other80CAnnual)
        val sec80CCD1B = compute80CCD1BUsage(npsAnnual)
        val basicPlusDa = summary.projectedAnnualBasicPay + summary.projectedAnnualDa
        val hra = computeHraExemption(summary.projectedAnnualHra, rentPaidAnnual, basicPlusDa, isMetro)
        val field = summary.projectedAnnualFieldAllowance

        val totalSec10 = hra + field
        val totalDeductions = sec80C.eligibleDeduction + sec80CCD1B.eligibleDeduction + totalSec10

        return TaxExemptionBreakdown(
            sec80C = sec80C,
            sec80CCD1B = sec80CCD1B,
            hraExemption = hra,
            fieldAllowanceExemption = field,
            totalSec10Exemptions = totalSec10,
            totalOldRegimeDeductions = totalDeductions,
        )
    }
}
