package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.TaxRegime

/**
 * Phase 5 (ADR-3/ADR-4): turns the always-computed [RegimeComparisonResult] and PCDA's own printed
 * figures into the insights the plan calls "worth paying for" -- the reconciliation between what will
 * actually be deducted and what will finally be owed, and the corollaries that follow from it. Every
 * number here is looked up or delegated (SSOT: [DualRegimeEngine], [DefenceTaxExemptionEngine]) --
 * no new slab, rebate, or cap logic is introduced.
 */
object TwoTrackReconciliationEngine {
    /** Below this, PCDA's figure and our engine's figure are treated as agreeing, not diverging. */
    private const val RECONCILIATION_THRESHOLD = 50.0

    /**
     * U1 (domain-layer instance): string-templating a [TaxRegime] enum directly stringifies to its
     * bare `.name` ("NEW"/"OLD"). This module can't reach composeApp's `AppStringsPremium` copy SSOT
     * (shared has no UI-layer dependency), so the full label is spelled out here in the same way this
     * file's own [belatedReturnTrapWarning] already writes "Old Tax Regime" as a literal.
     */
    private fun TaxRegime.regimeLabel(): String =
        when (this) {
            TaxRegime.OLD -> "Old Tax Regime"
            TaxRegime.NEW -> "New Tax Regime"
        }

    /**
     * Null when PCDA's own `totalTaxPayable` is unavailable (D3's guard leaves it `null` on genuine
     * implausibility) -- reconciling against a fabricated TDS figure would be exactly the bug D3 fixed.
     */
    fun reconcile(
        fySummary: FyTaxLedgerSummary,
        regimeComparison: RegimeComparisonResult,
        activePayslip: ParsedPayslip,
    ): TaxTrackReconciliation? {
        val pcdaAnnual = activePayslip.taxAndSavings?.totalTaxPayable ?: return null
        val tdsRegime = activePayslip.taxAndSavings.taxRegime

        val liabilityIsNew = regimeComparison.winnerRegime == TaxRegime.NEW.name
        val liabilityDetail = if (liabilityIsNew) regimeComparison.newRegime else regimeComparison.oldRegime
        val liabilityRegime = if (liabilityIsNew) TaxRegime.NEW else TaxRegime.OLD

        val delta = pcdaAnnual - liabilityDetail.totalTaxPayable
        val type =
            when {
                kotlin.math.abs(delta) < RECONCILIATION_THRESHOLD -> ReconciliationType.MATCHED
                delta > 0 -> ReconciliationType.REFUND_EXPECTED
                else -> ReconciliationType.TOP_UP_DUE
            }

        val pcdaText = TaxLedgerAggregator.formatIndianCurrency(pcdaAnnual)
        val deltaText = TaxLedgerAggregator.formatIndianCurrency(kotlin.math.abs(delta))
        val tdsRegimeLabel = tdsRegime.regimeLabel()
        val liabilityRegimeLabel = liabilityRegime.regimeLabel()
        val message =
            when (type) {
                ReconciliationType.MATCHED ->
                    "PCDA's withholding already matches your best achievable liability under the $liabilityRegimeLabel."
                ReconciliationType.REFUND_EXPECTED ->
                    "PCDA will deduct ₹$pcdaText under the $tdsRegimeLabel; filing under the $liabilityRegimeLabel recovers ₹$deltaText."
                ReconciliationType.TOP_UP_DUE ->
                    "PCDA will deduct ₹$pcdaText under the $tdsRegimeLabel; you will owe an additional ₹$deltaText at filing under the $liabilityRegimeLabel."
            }

        return TaxTrackReconciliation(
            financialYear = fySummary.financialYear,
            tdsTrackAnnual = pcdaAnnual,
            tdsTrackRegime = tdsRegime,
            liabilityTrackAnnual = liabilityDetail.totalTaxPayable,
            liabilityTrackRegime = liabilityRegime,
            deltaAmount = delta,
            reconciliationType = type,
            message = message,
        )
    }

    /**
     * Shown only when the Old Regime is the genuinely cheaper choice (i.e. there is an actual reason
     * to file under it) -- a belated return can never elect Old Regime, so this is the one trap that
     * turns "file under Old" from free advice into a deadline-dependent one.
     */
    fun belatedReturnTrapWarning(regimeComparison: RegimeComparisonResult): String? {
        if (regimeComparison.winnerRegime != TaxRegime.OLD.name) return null
        return "The Old Tax Regime is only available if you file your Income Tax Return by the original " +
            "due date under Section 139(1) -- it cannot be elected in a belated return."
    }

    /**
     * Non-null only under NEW with non-zero DSOP -- under OLD the contribution is already deductible,
     * so there is nothing "wasted" to disclose.
     */
    fun dsopWasteInsight(
        fySummary: FyTaxLedgerSummary,
        activeRegime: TaxRegime,
        oldRegimeComparison: RegimeComparisonResult,
    ): DsopWasteInsight? {
        if (activeRegime != TaxRegime.NEW) return null
        if (fySummary.projectedAnnualDsop <= 0.0) return null

        val totalContribution = fySummary.projectedAnnualDsop + fySummary.projectedAnnualAgif
        val capped = minOf(DefenceTaxExemptionEngine.LIMIT_80C, totalContribution)
        // oldRegimeComparison.financialYear already produced a resolved comparison above, so this FY
        // is guaranteed resolvable (ADR-2) -- the null branch is unreachable here.
        val oldRegimeMarginalRate =
            DualRegimeEngine.marginalRate(
                netTaxableIncome = oldRegimeComparison.oldRegime.netTaxableIncome,
                regime = TaxRegime.OLD,
                fy = oldRegimeComparison.financialYear,
            ) ?: 0.0
        val forgone = capped * oldRegimeMarginalRate

        val contributionText = TaxLedgerAggregator.formatIndianCurrency(totalContribution)
        val forgoneText = TaxLedgerAggregator.formatIndianCurrency(forgone)
        val message =
            "Your ₹$contributionText/yr DSOP + AGIF contribution earns ₹0 tax benefit under the New Regime -- " +
                "under the Old Regime it would have saved ₹$forgoneText/yr."

        return DsopWasteInsight(
            annualDsop = fySummary.projectedAnnualDsop,
            annualAgif = fySummary.projectedAnnualAgif,
            cappedContribution = capped,
            taxBenefitForgoneAnnual = forgone,
            message = message,
        )
    }

    /** Non-null only when YTD arrears are present -- nothing to disclose on a payslip with none. */
    fun arrearsTransparency(fySummary: FyTaxLedgerSummary): ArrearsTransparencyInsight? {
        if (fySummary.ytdArrears <= 0.0) return null
        val amountText = TaxLedgerAggregator.formatIndianCurrency(fySummary.ytdArrears)
        val message =
            "₹$amountText of your year-to-date income was one-off arrears -- added to your annual " +
                "projection as-is, not multiplied across the remaining months."
        return ArrearsTransparencyInsight(
            arrearsAmount = fySummary.ytdArrears,
            message = message,
        )
    }
}
