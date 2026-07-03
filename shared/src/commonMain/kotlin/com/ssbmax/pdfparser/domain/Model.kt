package com.ssbmax.pdfparser.domain

import kotlinx.serialization.Serializable

@Serializable
data class ParsedPayslip(
    val file: String,
    val year: Int,
    val monthNum: Int,
    val monthName: String,
    val dateStr: String,
    val officer: Officer,
    val earnings: Earnings,
    val deductions: Deductions,
    val ledgerBalances: LedgerBalances,
    val summary: PayslipSummary,
    val taxAndSavings: TaxAndSavings?,
    val rawEarnings: Map<String, Double> = emptyMap(),
    val rawDeductions: Map<String, Double> = emptyMap(),
    /**
     * Confidence sidecar (SSOT). Maps a field key (e.g. "basicPay", "incomeTax") to a 0..1 score
     * describing how certain the parser is about that value. Empty until the arithmetic solver
     * populates it (Phase 4); kept as a non-invasive sidecar so the 40+ raw double fields above
     * stay untouched (DRY). Consumed by the confidence-gated correction UI (Phase 5).
     */
    val fieldConfidence: Map<String, Float> = emptyMap(),
    /**
     * Provenance sidecar (SSOT, [FieldSource]). Maps a field key to which tier produced it —
     * `GEOMETRY` (the deterministic solver) or `GEMMA_FALLBACK` (Tier 6 on-device LLM, Android-only).
     * Empty until the solver/fallback populate it; lets the UI distinguish "solver was certain" from
     * "Gemma inferred this" instead of both collapsing into the same `fieldConfidence` number.
     */
    val fieldSource: Map<String, FieldSource> = emptyMap(),
    /** True when at least one field is below the review threshold and should be surfaced for user correction. */
    val needsReview: Boolean = false,
)

@Serializable
data class Officer(
    val name: String,
    val accountNo: String,
    val pan: String,
)

@Serializable
data class Earnings(
    val basicPay: Double = 0.0,
    val dearnessAllowance: Double = 0.0,
    val militaryServicePay: Double = 0.0,
    val transportAllowance: Double = 0.0,
    val transportAllowanceDa: Double = 0.0,
    val dressAllowance: Double = 0.0,
    val rationMoney: Double = 0.0,
    val specialForcesPay: Double = 0.0,
    val fieldAllowance: Double = 0.0,
    val childrenEducationAllowance: Double = 0.0,
    val houseRentAllowance: Double = 0.0,
    val riskHardshipAllowance: Double = 0.0,
    val nonPracticingAllowance: Double = 0.0,
    val adjBasicPay: Double = 0.0,
    val adjDa: Double = 0.0,
    val adjMsp: Double = 0.0,
    val adjTpta: Double = 0.0,
    val arrearsCea: Double = 0.0,
    val arrearsDa: Double = 0.0,
    val arrearsRation: Double = 0.0,
    val arrearsSpecialForces: Double = 0.0,
    val arrearsTpta: Double = 0.0,
    val arrearsTptaDa: Double = 0.0,
    val arrearsHra: Double = 0.0,
    val arrearsRiskHardship: Double = 0.0,
    val adjPayAndAllce: Double = 0.0,
    val adjFieldAllowance: Double = 0.0,
    val medicalAllowance: Double = 0.0,
    val adjTicketRecovery: Double = 0.0,
    val miscEarnings: Double = 0.0,
)

@Serializable
data class Deductions(
    val dsopSubscription: Double = 0.0,
    val agif: Double = 0.0,
    val incomeTax: Double = 0.0,
    val educationCess: Double = 0.0,
    val licenseFee: Double = 0.0,
    val furnitureRent: Double = 0.0,
    val waterCharges: Double = 0.0,
    val electricityCharges: Double = 0.0,
    val barrackDamage: Double = 0.0,
    val ticketRecovery: Double = 0.0,
    val recFieldAllowance: Double = 0.0,
    val recSpecialForces: Double = 0.0,
    val recoveryOfDebits: Double = 0.0,
    val aobf: Double = 0.0,
    val agifLoanRecovery: Double = 0.0,
    val miscDeductions: Double = 0.0,
)

@Serializable
data class LedgerBalances(
    val openingCreditBalance: Double = 0.0,
    val openingDebitBalance: Double = 0.0,
    val closingCreditBalance: Double = 0.0,
    val closingDebitBalance: Double = 0.0,
)

@Serializable
data class PayslipSummary(
    val grossPay: Double,
    val totalDeductions: Double,
    val netRemittance: Double,
)

@Serializable
data class TaxAndSavings(
    val grossSalaryYtd: Double = 0.0,
    val totalTaxableIncome: Double = 0.0,
    val standardDeduction: Double = 0.0,
    val netTaxableIncome: Double = 0.0,
    val totalTaxPayable: Double = 0.0,
    val taxDeductedYtd: Double = 0.0,
    val cessDeductedYtd: Double = 0.0,
    val dsopFund: DsopFund? = null,
)

@Serializable
data class DsopFund(
    val openingBalance: Double = 0.0,
    val subscriptionYtd: Double = 0.0,
    val refundYtd: Double = 0.0,
    val miscAdjYtd: Double = 0.0,
    val withdrawalYtd: Double = 0.0,
    val closingBalance: Double = 0.0,
)
