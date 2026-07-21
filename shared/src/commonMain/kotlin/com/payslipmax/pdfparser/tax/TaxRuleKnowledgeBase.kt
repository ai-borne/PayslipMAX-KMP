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
    val standardDeductionOld: Double = 50000.0,
    val standardDeductionNew: Double = 75000.0,
    val sec80CLimit: Double = 150000.0,
    val sec80CCD1BLimit: Double = 50000.0,
    val sec87ARebateMaxIncomeOld: Double = 500000.0,
    val sec87ARebateMaxIncomeNew: Double = 700000.0,
    val cessRatePct: Double = 4.0,
    val oldRegimeSlabs: List<TaxSlab>,
    val newRegimeSlabs: List<TaxSlab>,
    val defenceSection10Rules: List<Section10Rule>,
)

object TaxRuleKnowledgeBase {
    private val DEFAULT_DEFENCE_SECTION_10 =
        listOf(
            Section10Rule("HIGHLY_ACTIVE_FIELD", "Highly Active Field Area Allowance", 4200.0),
            Section10Rule("FIELD_AREA", "Field Area Allowance", 2700.0),
            Section10Rule("MODIFIED_FIELD", "Modified Field Area Allowance", 1000.0),
            Section10Rule("HIGH_ALTITUDE_L1", "High Altitude Allowance (Tier 1)", 1060.0),
            Section10Rule("HIGH_ALTITUDE_L2", "High Altitude Allowance (Tier 2)", 1600.0),
            Section10Rule("COUNTER_INSURGENCY", "Counter Insurgency Allowance", 3900.0),
            Section10Rule("ISLAND_DUTY", "Island Duty Allowance", 3250.0),
            Section10Rule("CHILDREN_EDU", "Children Education Allowance", 100.0),
            Section10Rule("HOSTEL_SUBSIDY", "Hostel Subsidy Allowance", 300.0),
        )

    private val OLD_REGIME_DEFAULT_SLABS =
        listOf(
            TaxSlab(0.0, 250000.0, 0.0),
            TaxSlab(250000.0, 500000.0, 0.05),
            TaxSlab(500000.0, 1000000.0, 0.20),
            TaxSlab(1000000.0, null, 0.30),
        )

    private val NEW_REGIME_DEFAULT_SLABS =
        listOf(
            TaxSlab(0.0, 300000.0, 0.0),
            TaxSlab(300000.0, 700000.0, 0.05),
            TaxSlab(700000.0, 1000000.0, 0.10),
            TaxSlab(1000000.0, 1200000.0, 0.15),
            TaxSlab(1200000.0, 1500000.0, 0.20),
            TaxSlab(1500000.0, null, 0.30),
        )

    private val RULES_MAP =
        mapOf(
            "2024-25" to
                TaxYearRules(
                    financialYear = "2024-25",
                    assessmentYear = "2025-26",
                    version = 20240701,
                    lastVerifiedDate = "2024-07-23",
                    sourceAuthority = "CBDT Notification / Finance Act 2024",
                    standardDeductionOld = 50000.0,
                    standardDeductionNew = 75000.0,
                    oldRegimeSlabs = OLD_REGIME_DEFAULT_SLABS,
                    newRegimeSlabs = NEW_REGIME_DEFAULT_SLABS,
                    defenceSection10Rules = DEFAULT_DEFENCE_SECTION_10,
                ),
            "2025-26" to
                TaxYearRules(
                    financialYear = "2025-26",
                    assessmentYear = "2026-27",
                    version = 20250201,
                    lastVerifiedDate = "2025-02-01",
                    sourceAuthority = "CBDT Rules / Finance Act 2024",
                    standardDeductionOld = 50000.0,
                    standardDeductionNew = 75000.0,
                    oldRegimeSlabs = OLD_REGIME_DEFAULT_SLABS,
                    newRegimeSlabs = NEW_REGIME_DEFAULT_SLABS,
                    defenceSection10Rules = DEFAULT_DEFENCE_SECTION_10,
                ),
            "2026-27" to
                TaxYearRules(
                    financialYear = "2026-27",
                    assessmentYear = "2027-28",
                    version = 20260201,
                    lastVerifiedDate = "2026-02-01",
                    sourceAuthority = "Rules FY 2026-27 (AY 2027-28) · Offline",
                    standardDeductionOld = 50000.0,
                    standardDeductionNew = 75000.0,
                    oldRegimeSlabs = OLD_REGIME_DEFAULT_SLABS,
                    newRegimeSlabs = NEW_REGIME_DEFAULT_SLABS,
                    defenceSection10Rules = DEFAULT_DEFENCE_SECTION_10,
                ),
        )

    fun getRulesForFy(fy: String): TaxYearRules {
        return RULES_MAP[fy] ?: RULES_MAP["2026-27"]!!
    }
}
