package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.TaxRegime

/**
 * ADR-4: regime switching is two decisions with different mechanisms and reversibility, confirmed
 * from the payslips themselves (PCDA(O) has advertised a self-service regime-change utility since
 * Jan 2023). Splitting them here means the UI (Phase 6) never has to re-derive which decision a given
 * savings figure actually belongs to.
 */
object RegimeDecisionPlanner {
    private const val PCDA_TRAP_WARNING = "Cannot be changed again mid-year once submitted (CBDT Circular 4/2023)."
    private const val ITR_OLD_REGIME_TRAP_WARNING = "Old Regime is unavailable in a belated return -- file by the due date."

    /**
     * Null when the currently active regime already is the cheaper one -- there is no decision to
     * present, matching the previous "switch_regime" opportunity's own gating condition.
     */
    fun buildRegimeDecisionPlan(
        regimeComparison: RegimeComparisonResult,
        activeRegime: TaxRegime,
    ): RegimeDecisionPlan? {
        if (regimeComparison.winnerRegime == activeRegime.name) return null
        if (regimeComparison.annualSavings <= 0.0) return null

        val bestRegime = regimeComparison.winnerRegime
        val savingsText = TaxLedgerAggregator.formatIndianCurrency(regimeComparison.annualSavings)

        val intimation =
            RegimeDecision(
                id = "pcda_intimation_regime",
                title = "Intimate $bestRegime Regime to PCDA",
                mechanism = "PCDA(O) self-service regime-change utility",
                reversibleMidYear = false,
                trapWarning = PCDA_TRAP_WARNING,
                action = "Submit the regime-change intimation on the PCDA(O) portal so future months withhold under $bestRegime.",
            )

        val election =
            RegimeDecision(
                id = "itr_election_regime",
                title = "Elect $bestRegime Regime in your ITR",
                mechanism = "Return filed under Section 139(1)",
                reversibleMidYear = true,
                trapWarning = if (bestRegime == TaxRegime.OLD.name) ITR_OLD_REGIME_TRAP_WARNING else null,
                action = "File your ITR under the $bestRegime Tax Regime -- independent of what PCDA withholds, saving ~₹$savingsText/year.",
            )

        return RegimeDecisionPlan(intimation, election)
    }

    /**
     * Compares [ParsedPayslip.taxAndSavings]' `taxRegime` across every payslip uploaded within [fy], in
     * calendar order -- distinct from reading a single payslip's own regime, since a mid-year change is
     * only visible across the ledger.
     */
    fun detectMidYearRegimeChange(
        payslips: List<ParsedPayslip>,
        fy: String,
    ): MidYearRegimeChangeInsight {
        val fyPayslips =
            payslips.filter { TaxLedgerAggregator.computeFinancialYear(it.year, it.monthNum) == fy }
                .sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })

        val regimesInOrder = fyPayslips.mapNotNull { it.taxAndSavings?.taxRegime }
        val distinctRegimes = regimesInOrder.distinct()

        if (distinctRegimes.size <= 1) {
            return MidYearRegimeChangeInsight(
                detected = false,
                regimesSeen = distinctRegimes,
                changeMonthNum = null,
                message = null,
            )
        }

        val changeIndex = regimesInOrder.zipWithNext().indexOfFirst { (previous, current) -> previous != current }
        val changeMonth = if (changeIndex >= 0) fyPayslips[changeIndex + 1].monthNum else null
        val message =
            "Your tax regime changed mid-year in FY $fy (from ${regimesInOrder.first()} to ${regimesInOrder.last()}) -- " +
                "PCDA's TDS regime intimation cannot be reversed again this FY (CBDT Circular 4/2023)."

        return MidYearRegimeChangeInsight(
            detected = true,
            regimesSeen = distinctRegimes,
            changeMonthNum = changeMonth,
            message = message,
        )
    }
}
