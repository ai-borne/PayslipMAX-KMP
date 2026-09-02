package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.TaxRegime
import kotlinx.serialization.Serializable

/**
 * ADR-3's two-track model: PCDA's own printed figure (what will actually be withheld) is never
 * silently replaced by our engine's number, or vice versa -- both are shown, and the gap between
 * them is the reconciliation this class exists to explain (refund on filing, or a top-up owed).
 */
@Serializable
enum class ReconciliationType {
    REFUND_EXPECTED,
    TOP_UP_DUE,
    MATCHED,
}

/**
 * [tdsTrackAnnual]/[tdsTrackRegime] answer "what will PCDA actually deduct?" (source: the payslip's
 * own printed `totalTaxPayable`). [liabilityTrackAnnual]/[liabilityTrackRegime] answer "what will I
 * finally owe?" under current law for whichever regime [RegimeComparisonResult.winnerRegime] shows is
 * genuinely cheaper -- not necessarily the regime PCDA is withholding under, since ADR-4 says the ITR
 * election is independent of what PCDA was told.
 */
@Serializable
data class TaxTrackReconciliation(
    val financialYear: String,
    val tdsTrackAnnual: Double,
    val tdsTrackRegime: TaxRegime,
    val liabilityTrackAnnual: Double,
    val liabilityTrackRegime: TaxRegime,
    val deltaAmount: Double,
    val reconciliationType: ReconciliationType,
    val message: String,
)

/** DSOP+AGIF contributions that earn zero deduction under the New Regime (D9's contradiction inverted). */
@Serializable
data class DsopWasteInsight(
    val annualDsop: Double,
    val annualAgif: Double,
    val cappedContribution: Double,
    val taxBenefitForgoneAnnual: Double,
    val message: String,
)

/** Discloses the exact YTD arrears amount added back verbatim (never annualised) into the projection. */
@Serializable
data class ArrearsTransparencyInsight(
    val arrearsAmount: Double,
    val message: String,
)

/** Detects a regime change across the FY's uploaded payslips -- distinct from a single-payslip read. */
@Serializable
data class MidYearRegimeChangeInsight(
    val detected: Boolean,
    val regimesSeen: List<TaxRegime>,
    val changeMonthNum: Int?,
    val message: String?,
)

/**
 * One half of the ADR-4 split: an actionable decision with its own mechanism, reversibility, and
 * (optionally) a trap the user must know about before acting on it.
 */
@Serializable
data class RegimeDecision(
    val id: String,
    val title: String,
    val mechanism: String,
    val reversibleMidYear: Boolean,
    val trapWarning: String?,
    val action: String,
)

/**
 * ADR-4: the crude single "Switch regime" opportunity is really two decisions with different
 * reversibility -- the PCDA(O) withholding intimation (irreversible mid-year, CBDT Circular 4/2023)
 * and the ITR election (independent, reversible until filing, but Old Regime is trapped by a belated
 * return). Never collapsed back into one.
 */
@Serializable
data class RegimeDecisionPlan(
    val pcdaIntimationDecision: RegimeDecision,
    val itrElectionDecision: RegimeDecision,
)
