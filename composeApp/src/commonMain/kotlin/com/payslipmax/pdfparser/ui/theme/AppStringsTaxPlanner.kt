package com.payslipmax.pdfparser.ui.theme

/**
 * Phase 6 (Tax Planner Gold Standard) copy. Kept separate from [AppStringsPremium] (already at the
 * 300-LOC ceiling) per Rule 4 -- every new tax-screen string, including the bare rupee glyph, lives
 * here so no `ui/screens/Tax*.kt` composable types a literal string of its own.
 */
object AppStringsTaxPlanner {
    const val rupeeSymbol = "₹"
    const val bestBadge = "★ Best"

    // Data-as-of banner (Phase 8, U2): the screen previously had no on-screen indicator of which
    // month's payslip is active, silently inheriting whatever the Dashboard's month-picker last set.
    const val dataAsOfPrefix = "Data as of: "

    // Hero verdict card (replaces the deleted Peer Benchmark card, D15)
    const val heroTitle = "Your Tax Verdict"
    const val heroLiabilityLabel = "Best Achievable Annual Liability"
    const val heroRegimeWinnerPrefix = "Regime "
    const val heroRegimeWinnerSuffix = " wins"
    const val heroNextMonthTdsLabel = "Next Month's TDS ≈ ₹"
    const val heroNextMonthNetPayLabel = "Next Month's Net Pay ≈ ₹"
    const val heroLowCoveragePrefix = "Estimate based on "
    const val heroLowCoverageSuffix = " of 12 months -- range: "
    const val heroRangeSeparator = " – "

    // FY coverage header card
    const val fyMonthsParsedPrefix = "🟢 "
    const val fyMonthSingular = " Month Parsed (Projected)"
    const val fyMonthPlural = " Months Parsed (Projected)"

    // PCDA Official Computation card
    const val pcdaCardTitle = "PCDA Official Computation"
    const val pcdaCardSubtitle = "As printed on your payslip, page 4"
    const val pcdaGrossYtdLabel = "Gross Salary YTD: ₹"
    const val pcdaTaxableIncomeLabel = "Total Taxable Income: ₹"
    const val pcdaStandardDeductionLabel = "Standard Deduction: ₹"
    const val pcdaNetTaxableLabel = "Net Taxable Income: ₹"
    const val pcdaTotalTaxPayableLabel = "Total Tax Payable: ₹"
    const val pcdaTaxDeductedYtdLabel = "Income Tax Deducted YTD: ₹"
    const val pcdaCessDeductedYtdLabel = "Cess Deducted YTD: ₹"
    const val pcdaTaxUnavailableNote = "PCDA's printed Total Tax Payable was implausible on this payslip and is withheld rather than guessed at."

    // Reconciliation delta card (ADR-3/ADR-4)
    const val reconciliationCardTitle = "TDS vs Final Liability"
    const val reconciliationTdsTrackLabel = "PCDA Will Deduct (TDS)"
    const val reconciliationLiabilityTrackLabel = "You'll Finally Owe (Liability)"
    const val belatedReturnWarningTitle = "⚠️ Belated Return Trap"
    const val midYearChangeTitle = "🔁 Mid-Year Regime Change"
    const val arrearsTransparencyTitle = "Arrears Transparency"

    // DSOP-waste insight card
    const val dsopWasteCardTitle = "DSOP + AGIF: Zero Benefit Under New Regime"

    // TDS runway spike banner
    const val tdsSpikeToLabel = " to ₹"
    const val tdsSpikeSuffix = " per month in remaining FY!"
    const val tdsPerMonthSuffix = "/mo ("
    const val tdsMonthsRemainingSuffix = " mos remaining)"

    // Regime battle card: explicit cess/surcharge breakdown (D17)
    const val regimeBaseTaxLabel = "Base Tax: ₹"
    const val regimeSurchargeLabel = "Surcharge: ₹"
    const val regimeCessLabel = "Cess: ₹"

    // Narrative ledger card
    const val nudgeBannerPrefix = "💡 "

    // Rule pack footer (Phase 7, D17): version/verified-date sourced from TaxRuleKnowledgeBase, never
    // a hardcoded "Version 2026.1" disconnected from the rule pack that actually computed the numbers.
    const val ruleFooterPrefix = "🛡️ Tax Engine: CBDT Rules FY "
    const val ruleFooterAySeparator = " (AY "
    const val ruleFooterVersionSeparator = ") · Version "
    const val ruleFooterVerifiedSeparator = " (Verified "
    const val ruleFooterSuffix = ") · 100% Offline Secured"

    // ADR-2 fail-loud UI state: shown instead of every regime/liability card when the active FY has no
    // resolvable rule pack, rather than silently computing off the nearest known FY's numbers.
    const val rulesUnavailableTitle = "⚠️ Tax rules unavailable for FY "
    const val rulesUnavailableBodyPrefix = "We don't have a verified rule pack for this financial year yet. The nearest year we do have rules for is "
    const val rulesUnavailableBodySuffix = "."

    // Advice disclaimer (Phase 7): appropriate once the screen recommends executable financial actions.
    const val adviceDisclaimer =
        "This is an estimate, not tax or financial advice. Verify your figures and any regime switch with PCDA(O) or a Chartered Accountant before acting."
}
