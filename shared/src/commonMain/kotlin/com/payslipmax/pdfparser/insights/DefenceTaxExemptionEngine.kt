package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.TaxRegime
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

/**
 * [regime]-conditional: under [TaxRegime.NEW] the old-regime-only sections (80C, 80CCD(1B), HRA,
 * Sec 10(14) field/risk-hardship allowances) are not deductible in law, so their eligible/exempt
 * amounts are zeroed and the reason recorded in [unavailableUnderRegime] (D10) -- keyed by the same
 * section id used in tests, e.g. "sec80C". Never a silent zero: a caller that sees ₹0 can always look
 * up *why*. [totalOldRegimeDeductions] under NEW is therefore also 0 and must not be reused to drive
 * the OLD-regime hypothetical in [DualRegimeEngine.compareRegimes] -- that comparison always needs the
 * regime-neutral (capped, not gated) figure, i.e. [DefenceTaxExemptionEngine.extractExemptions] called
 * with the default `activeRegime = TaxRegime.OLD`.
 */
@Serializable
data class TaxExemptionBreakdown(
    val sec80C: Section80CResult,
    val sec80CCD1B: Section80CCD1BResult,
    val hraExemption: Double,
    val fieldAllowanceExemption: Double,
    val totalSec10Exemptions: Double,
    val totalOldRegimeDeductions: Double,
    val regime: TaxRegime = TaxRegime.OLD,
    val unavailableUnderRegime: Map<String, String> = emptyMap(),
    /** Open Item 2: true when [fieldAllowanceExemption] used the conservative fallback cap because the
     * generic field-allowance bucket couldn't be resolved to a specific Rule 2BB tier from parsed data. */
    val fieldCapIsConservativeAssumption: Boolean = false,
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

    /** D10: not available under Section 115BAC (New Regime) -- keyed by the section id it explains. */
    const val REASON_NOT_AVAILABLE_NEW_REGIME = "Not available under the New Tax Regime (Section 115BAC)"

    fun extractExemptions(
        summary: FyTaxLedgerSummary,
        rentPaidAnnual: Double = 0.0,
        other80CAnnual: Double = 0.0,
        npsAnnual: Double = 0.0,
        isMetro: Boolean = false,
        activeRegime: TaxRegime = TaxRegime.OLD,
    ): TaxExemptionBreakdown {
        val sec80C = compute80CUsage(summary.projectedAnnualDsop, summary.projectedAnnualAgif, other80CAnnual)
        val sec80CCD1B = compute80CCD1BUsage(npsAnnual)
        val basicPlusDa = summary.projectedAnnualBasicPay + summary.projectedAnnualDa
        val hra = computeHraExemption(summary.projectedAnnualHra, rentPaidAnnual, basicPlusDa, isMetro)
        val capped = Section10CapPolicy.capExemptions(summary.projectedAnnualFieldAllowance, summary.projectedAnnualRiskHardshipAllowance)

        if (activeRegime == TaxRegime.NEW) {
            return TaxExemptionBreakdown(
                sec80C = sec80C.copy(eligibleDeduction = 0.0, headroom = 0.0),
                sec80CCD1B = sec80CCD1B.copy(eligibleDeduction = 0.0, headroom = 0.0),
                hraExemption = 0.0,
                fieldAllowanceExemption = 0.0,
                totalSec10Exemptions = 0.0,
                totalOldRegimeDeductions = 0.0,
                regime = TaxRegime.NEW,
                unavailableUnderRegime =
                    mapOf(
                        "sec80C" to REASON_NOT_AVAILABLE_NEW_REGIME,
                        "sec80CCD1B" to REASON_NOT_AVAILABLE_NEW_REGIME,
                        "hra" to REASON_NOT_AVAILABLE_NEW_REGIME,
                        "sec10Field" to REASON_NOT_AVAILABLE_NEW_REGIME,
                    ),
            )
        }

        val totalSec10 = hra + capped.exempt
        val totalDeductions = sec80C.eligibleDeduction + sec80CCD1B.eligibleDeduction + totalSec10

        return TaxExemptionBreakdown(
            sec80C = sec80C,
            sec80CCD1B = sec80CCD1B,
            hraExemption = hra,
            fieldAllowanceExemption = capped.exempt,
            totalSec10Exemptions = totalSec10,
            totalOldRegimeDeductions = totalDeductions,
            regime = TaxRegime.OLD,
            fieldCapIsConservativeAssumption = capped.isConservativeAssumption,
        )
    }
}
