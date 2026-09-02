package com.payslipmax.pdfparser.tax

import com.payslipmax.pdfparser.tax.rules.NewRegimeSlabTable
import com.payslipmax.pdfparser.tax.rules.OldRegimeSlabTable
import com.payslipmax.pdfparser.tax.rules.Section10ExemptionTable

/**
 * FY-versioned tax rules, FY 2015-16 -> FY 2026-27 (ADR-2). [getRulesForFy] and [resolve] never
 * silently substitute another FY's rules for an unknown FY -- callers must handle [TaxRuleResolution]
 * explicitly, or accept the nearest-known-FY fallback documented on [getRulesForFy].
 */
object TaxRuleKnowledgeBase {
    private const val LAST_VERIFIED = "2026-08-11"

    private fun buildRules(
        fy: String,
        ay: String,
        version: Int,
        sourceAuthority: String,
        stdDedOld: Double,
        stdDedNew: Double,
        rebateMaxIncomeNew: Double,
        rebateCapNew: Double,
    ): TaxYearRules =
        TaxYearRules(
            financialYear = fy,
            assessmentYear = ay,
            version = version,
            lastVerifiedDate = LAST_VERIFIED,
            sourceAuthority = sourceAuthority,
            standardDeductionOld = stdDedOld,
            standardDeductionNew = stdDedNew,
            sec87ARebateMaxIncomeNew = rebateMaxIncomeNew,
            sec87ARebateCapNew = rebateCapNew,
            oldRegimeSlabs = OldRegimeSlabTable.forFy(fy),
            newRegimeSlabs = NewRegimeSlabTable.forFy(fy),
            defenceSection10Rules = Section10ExemptionTable.DEFAULT_DEFENCE_SECTION_10,
        )

    // New-regime rebate pre-FY2023-24 (Section 115BAC original, FY2020-21 onward) mirrored the old
    // regime's 5L/12,500 rebate; the regime did not exist before FY2020-21, so FY2015-16 -> FY2019-20
    // carry the same placeholder values purely to satisfy the schema (never reachable: no payslip in
    // the corpus elects NEW regime before FY2023-24, and no UI surface offers it either).
    private val RULES_MAP =
        listOf(
            buildRules("2015-16", "2016-17", 20150101, "Finance Act 2015", 0.0, 0.0, 500000.0, 12500.0),
            buildRules("2016-17", "2017-18", 20160101, "Finance Act 2016", 0.0, 0.0, 500000.0, 12500.0),
            buildRules("2017-18", "2018-19", 20170101, "Finance Act 2017", 0.0, 0.0, 500000.0, 12500.0),
            buildRules("2018-19", "2019-20", 20180101, "Finance Act 2018", 40000.0, 0.0, 500000.0, 12500.0),
            buildRules("2019-20", "2020-21", 20190101, "Finance Act 2019", 50000.0, 0.0, 500000.0, 12500.0),
            buildRules("2020-21", "2021-22", 20200101, "Finance Act 2020 (Sec 115BAC introduced)", 50000.0, 0.0, 500000.0, 12500.0),
            buildRules("2021-22", "2022-23", 20210101, "Finance Act 2021", 50000.0, 0.0, 500000.0, 12500.0),
            buildRules("2022-23", "2023-24", 20220101, "Finance Act 2022", 50000.0, 0.0, 500000.0, 12500.0),
            buildRules("2023-24", "2024-25", 20230201, "Finance Act 2023 (new regime default + simplified slabs)", 50000.0, 50000.0, 700000.0, 25000.0),
            buildRules("2024-25", "2025-26", 20240723, "Finance (No.2) Act 2024", 50000.0, 75000.0, 700000.0, 25000.0),
            buildRules("2025-26", "2026-27", 20250201, "Finance Act 2025", 50000.0, 75000.0, 1200000.0, 60000.0),
            buildRules("2026-27", "2027-28", 20260201, "Finance Act 2025 (carried forward, no FY2026-27 Finance Act yet)", 50000.0, 75000.0, 1200000.0, 60000.0),
        ).associateBy { it.financialYear }

    private val KNOWN_FYS = RULES_MAP.keys.sorted()
    private const val DEFAULT_FALLBACK_FY = "2026-27"

    /** Explicit resolution (ADR-2): never silently substitutes rules for an unrecognised FY. */
    fun resolve(fy: String): TaxRuleResolution {
        val rules = RULES_MAP[fy]
        return if (rules != null) {
            TaxRuleResolution.Resolved(rules)
        } else {
            val nearest = KNOWN_FYS.minByOrNull { kotlin.math.abs(startYearOf(it) - startYearOf(fy)) } ?: DEFAULT_FALLBACK_FY
            TaxRuleResolution.OutOfRange(fy, nearest)
        }
    }

    private fun startYearOf(fy: String): Int = fy.substringBefore("-").toIntOrNull() ?: Int.MIN_VALUE

    /**
     * Convenience accessor for callers that cannot handle [TaxRuleResolution.OutOfRange] (legacy call
     * sites being migrated) -- falls back to the nearest known FY's rules rather than crashing. New
     * call sites should prefer [resolve] and surface out-of-range FYs to the user (ADR-2, "fail loud").
     */
    fun getRulesForFy(fy: String): TaxYearRules =
        when (val resolution = resolve(fy)) {
            is TaxRuleResolution.Resolved -> resolution.rules
            is TaxRuleResolution.OutOfRange -> RULES_MAP[resolution.nearestKnownFy] ?: error("TaxRuleKnowledgeBase initialization error: no tax rules available")
        }
}
