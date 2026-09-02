package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.tax.rules.Section10ExemptionTable

/**
 * Result of applying Rule 2BB monthly exemption caps to a defence allowance bucket.
 * [isConservativeAssumption] is true only for the generic field-allowance bucket, where the parsed
 * PCDA code cannot be resolved to a specific Rule 2BB tier (see class doc, Plan Open Item 2).
 */
data class CappedSection10Result(
    val exempt: Double,
    val cappedFieldAmount: Double,
    val cappedRiskHardshipAmount: Double,
    val isConservativeAssumption: Boolean,
)

/**
 * Maps the two Sec 10(14) defence-allowance buckets [TaxLedgerAggregator] produces to Rule 2BB
 * categories and applies the notified monthly exemption cap (D8) -- replacing the previous uncapped
 * pass-through.
 *
 * `RH11`-`RH33` and `SICHA` all collapse into the single structured `riskHardshipAllowance` bucket at
 * parse time (see `PayslipPatternConfig.creditKeysMapping`), which corresponds to the notified
 * "Special Compensatory (Highly Active Field Area) Allowance" Rule 2BB category.
 *
 * The generic `fieldAllowance` bucket (PCDA code "FD") cannot be resolved to a specific field-area
 * tier from parsed data alone -- Plan Open Item 2 directs applying the lowest ("most conservative")
 * field-area cap and disclosing the assumption rather than guessing generously.
 */
object Section10CapPolicy {
    private val rulesByKey = Section10ExemptionTable.DEFAULT_DEFENCE_SECTION_10.associateBy { it.allowanceKey }

    private const val RISK_HARDSHIP_CATEGORY_KEY = "HIGHLY_ACTIVE_FIELD"
    private const val CONSERVATIVE_FIELD_CATEGORY_KEY = "MODIFIED_FIELD"

    fun capExemptions(
        annualFieldAllowanceReceived: Double,
        annualRiskHardshipReceived: Double,
    ): CappedSection10Result {
        val cappedRiskHardship = minOf(annualRiskHardshipReceived, annualCapFor(RISK_HARDSHIP_CATEGORY_KEY))
        val cappedField = minOf(annualFieldAllowanceReceived, annualCapFor(CONSERVATIVE_FIELD_CATEGORY_KEY))

        return CappedSection10Result(
            exempt = cappedField + cappedRiskHardship,
            cappedFieldAmount = cappedField,
            cappedRiskHardshipAmount = cappedRiskHardship,
            isConservativeAssumption = annualFieldAllowanceReceived > 0.0,
        )
    }

    private fun annualCapFor(key: String): Double {
        val monthlyLimit = rulesByKey[key]?.monthlyExemptionLimit ?: 0.0
        return monthlyLimit * 12.0
    }
}
