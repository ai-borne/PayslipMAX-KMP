package com.payslipmax.pdfparser.ui.theme

object InsightsStrings {
    // Wellness chip, month selector & score driver labels
    const val wellnessChipLabel = "Pay Health"
    const val wellnessSavingsRateLabel = "Savings Rate:"
    const val wellnessNoIssuesBonus = "Clean payslip — no anomalies detected"
    const val wellnessImproveSavingsRate = "Increase DSOP subscription to improve savings rate"
    const val wellnessImproveMissingAllowance = "File representation to PCDA(O) for missing allowance"
    const val wellnessImproveSalaryLoss = "Raise official representation to recover salary loss"
    const val wellnessImproveDeductionSpike = "Verify IT computation sheet with PAO"
    const val wellnessImproveTptaEntitlement = "File TPTA claim with supporting documents"
    const val wellnessDsopNonCompliance = "Increase DSOP subscription to the minimum required"
    const val wellnessTitleMissingAllowance = "Missing Allowance:"
    const val wellnessTitleSalaryLoss = "Salary Loss Detected"
    const val wellnessTitleDeductionSpike = "Deduction Spike:"
    const val wellnessTitleTpta = "TPTA Entitlement Advisory"
    const val wellnessTitleDsop = "DSOP Non-Compliance"

    // Adaptive Hero card
    const val heroRecoverySectionTitle = "Recovery Opportunity"
    const val heroRecoverySubLabel = "recoverable from PCDA"
    const val heroRecoveryCtaLabel = "Draft Representation to PCDA(O)"
    const val heroRecoveryMoreIssuesSuffix = "more issue(s)"
    const val heroWealthSectionTitle = "Wealth Optimization"
    const val heroWealthSubLabel = "tax saving available"
    const val heroWealthCtaLabel = "See how →"
    const val heroWealthRegimeDisclaimer = "Old regime est. — verify with PAO if on new regime"
    const val heroWealthRegimeNewActive = "New Tax Regime active — no further standard optimizations."

    // Tax Planner real numbers
    const val taxPlanningOldRegimeEst = "(old regime est.)"
    const val taxPlanningNewRegimeEst = "(new regime)"
    const val taxPlanningEstTaxSaved = "Est. tax saved: ₹"
    const val taxPlanningHeadroom = "Headroom: ₹"
    const val taxPlanningRegimeDisclaimer = "Figures assume old-regime deductions. Verify with PAO if on new regime."
    const val taxPlanningRegimeDisclaimerNew = "Figures assume New Tax Regime. Standard deductions are auto-applied by PCDA."

    // Premium tools value props
    const val premiumToolsTaxPlannerValueProp = "See 80C/NPS headroom & estimated tax savings"
    const val premiumToolsDsopValueProp = "Project your DSOP corpus at retirement"
    const val premiumToolsOpenLabel = "Open"
    const val premiumToolsDraftClaimsIcon = "📋"
    const val premiumToolsTaxPlannerIcon = "📊"
    const val premiumToolsDsopIcon = "📈"

    // Pro features teaser card
    const val proTeaserAiDetail = "CA-grade AI financial audit powered by Gemini"
    const val proTeaserToolsDetail = "Draft Claims · Tax Planner · DSOP Simulator"

    // Accessibility content descriptions
    const val wellnessChipExpandDesc = "Expand score drivers"
    const val wellnessChipCollapseDesc = "Collapse score drivers"

    // Health KPI card (status bands, expanded breakdown sections)
    const val healthStatusExcellent = "Excellent"
    const val healthStatusHealthy = "Healthy"
    const val healthStatusFair = "Fair"
    const val healthStatusNeedsAttention = "Needs Attention"
    const val healthStatusCritical = "Critical"
    const val positiveSignalsTitle = "Positive Signals"
    const val watchItemsTitle = "Watch Items"
    const val opportunityTitle = "Opportunity"
    const val wellnessTrendSinceLastPayslip = "last payslip"
    const val wellnessTrendImprovedPrefix = "↑ Improved by"
    const val wellnessTrendDownPrefix = "↓ Down"
    const val wellnessTrendPointsSince = "points since"
    const val wellnessPositiveDriverPrefix = "✅ "
    const val wellnessWatchDriverPrefix = "⚠ "

    // Improve path prefix (SSOT for the "→" arrow used in driver rows)
    const val wellnessImprovePathPrefix = "→ "

    // Pay breakdown chart (stacked Net/DSOP/Tax/Other across the trailing window)
    const val sixMonthBreakdownTitle = "6-Month Pay Breakdown"
    const val monthBreakdownTitleSuffix = "-Month Pay Breakdown"
    const val dateRangeSeparator = " – "

    // AI summary truncation limit
    const val aiSummaryMaxLength = 200

    // Refactor Strings
    const val keyFindingsTitle = "Key Findings"
    const val aiHighlightsTitle = "AI Highlights"
    const val premiumIntelligenceTitle = "Premium Intelligence"
    const val premiumIntelligencePrice = "₹99 / Year"
    const val estimatedOpportunityLabel = "Estimated Opportunity:"
    const val potentialTaxSavingsTitle = "Potential Tax Savings Found"
    const val potentialRecoveryOpportunityTitle = "Recovery Opportunity Found"
    const val unlockFullRecommendationLabel = "Unlock full recommendation"
    const val unlockFullRepresentationsLabel = "Unlock full PCDA(O) representations"

    // Premium teaser defaults & activated status
    const val premiumTeaserDefaultTitle = "Premium Financial Analysis Found"
    const val premiumTeaserDefaultOpportunity = "Complete Financial Toolkit"
    const val premiumTeaserDefaultInsight = "Detailed projections, anomaly audits, and claims generators ready."
    const val premiumTeaserDefaultButton = "Unlock Premium Intelligence"
    const val premiumActivatedSuffix = " (Activated)"

    // Wealth Optimization card locked/teaser state (WEALTH_OPTIMIZATION gate)
    const val wealthLockedTitle = "🔒 Wealth Optimization"
    const val wealthLockedBody = "80C/NPS headroom & DSOP corpus projections"
    const val wealthLockedCta = "Unlock Wealth Optimization"

    // Advanced anomaly checks (ANOMALY_DETECTION gate, D6) — category labels + locked teaser copy.
    // Free tier keeps SALARY_LOSS/DEDUCTION_SPIKE via the health score; these labels name the PRO checks.
    const val anomalyLabelSalaryLoss = "Salary Loss"
    const val anomalyLabelDeductionSpike = "Deduction Spike"
    const val anomalyLabelMissingAllowance = "Missing Allowance"
    const val anomalyLabelTptaEntitlement = "TPTA Entitlement"
    const val anomalyLabelArrearsAudit = "DA Arrears"
    const val anomalyLabelDsopCompliance = "DSOP Compliance"
    const val anomalyLabelDsopMilestone = "DSOP Milestone"
    const val anomalyLabelTaxProjection = "Tax Projection"
    const val anomalyLabelRentRecoveryRisk = "Quarters / Rent Risk"
    const val anomalyLabelDebitRecovery = "Debit Recovery"
    const val anomalyLabelUnknown = "Financial Check"

    const val advancedAnomaliesTitle = "Advanced Anomaly Checks"
    const val advancedAnomaliesLockedTitle = "🔒 Advanced Anomaly Checks"
    const val advancedAnomaliesLockedBody = "Detailed audit findings and recovery amounts are part of PRO."
    const val advancedAnomaliesLockedCountSuffix = "advanced check(s) found on this payslip"
    const val advancedAnomaliesUnlockCta = "Unlock Anomaly Detection"
    const val advancedAnomaliesLabelSeparator = " · "
}
