package com.payslipmax.pdfparser.tax

import kotlinx.serialization.Serializable

@Serializable
data class TaxSlab(
    val minIncome: Double,
    val maxIncome: Double?,
    val taxRatePct: Double,
)

@Serializable
data class Section10Rule(
    val allowanceKey: String,
    val ruleName: String,
    val monthlyExemptionLimit: Double,
    val sectionRef: String = "Section 10(14)",
)

@Serializable
data class TaxYearRules(
    val financialYear: String,
    val assessmentYear: String,
    val version: Int,
    val lastVerifiedDate: String,
    val sourceAuthority: String,
    val standardDeductionOld: Double,
    val standardDeductionNew: Double,
    val sec80CLimit: Double = 150000.0,
    val sec80CCD1BLimit: Double = 50000.0,
    val sec87ARebateMaxIncomeOld: Double = 500000.0,
    val sec87ARebateCapOld: Double = 12500.0,
    val sec87ARebateMaxIncomeNew: Double,
    val sec87ARebateCapNew: Double,
    val cessRatePct: Double = 4.0,
    val oldRegimeSlabs: List<TaxSlab>,
    val newRegimeSlabs: List<TaxSlab>,
    val defenceSection10Rules: List<Section10Rule>,
)

/** Result of resolving tax rules for a financial year (ADR-2) -- never silently substitutes another FY's rules. */
sealed class TaxRuleResolution {
    data class Resolved(
        val rules: TaxYearRules,
    ) : TaxRuleResolution()

    data class OutOfRange(
        val requestedFy: String,
        val nearestKnownFy: String,
    ) : TaxRuleResolution()
}
