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

    // Pay Health chip opportunity CTA
    const val heroWealthCtaLabel = "See how →"

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

    // AI summary truncation limit
    const val aiSummaryMaxLength = 200

    // Refactor Strings
    const val premiumIntelligencePrice = "₹99 / Year"

    // Premium teaser defaults & activated status
    const val premiumActivatedSuffix = " (Activated)"

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
    const val advancedAnomaliesLockedCountSuffix = "advanced check(s) found on this payslip"
    const val advancedAnomaliesLabelSeparator = " · "

    // Smart Insights (redesign) — first-statement fallback + action copy for anomaly types that have
    // no existing wellness-driver action text (see InsightsWellnessLogic.kt for the reused ones).
    const val smartInsightsFirstStatementTitle = "First Statement Uploaded"
    const val smartInsightsFirstStatementBody =
        "This is your first tracked payslip — Smart Insights will start comparing month-over-month changes from your next upload."
    const val smartInsightsActionTaxProjection = "Review your projected annual tax liability in Tax Planner"
    const val smartInsightsSectionTitle = "Smart Insights"

    // Folded free findings — the useful signal from the retired KeyFindings/AiHighlights sections,
    // surfaced as low-priority INFO cards in Smart Insights instead of being deleted outright.
    const val freeFindingLargestDeductionTitle = "Largest Deduction"
    const val freeFindingLargestDeductionTax = "Income Tax is your largest deduction this month"
    const val freeFindingLargestDeductionDsop = "DSOP Subscription is your largest deduction this month"
    const val freeFindingComponentChangeTitle = "Biggest Pay Component Change"
    const val freeFindingComponentBasic = "Basic Pay"
    const val freeFindingComponentDa = "Dearness Allowance"
    const val freeFindingComponentTpta = "Transport Allowance"
    const val freeFindingComponentHra = "House Rent Allowance"
    const val freeFindingIncreasedSuffix = "increased compared to last month"
    const val freeFindingDecreasedSuffix = "decreased compared to last month"

    // Severity chip labels (InsightCard) — short, scannable badges over the full enum name.
    const val severityLabelInfo = "Info"
    const val severityLabelWarning = "Watch"
    const val severityLabelImportant = "Alert"
    const val severityLabelOpportunity = "Opportunity"

    // Monthly Snapshot (net-pay hero + metrics + Pay Health chip)
    const val snapshotNetPayLabel = "Net Pay"
    const val snapshotGrossPayLabel = "Gross Pay"
    const val snapshotDeductionsLabel = "Deductions"
    const val snapshotTaxLabel = "Income Tax"

    // Pay Trend chart (net-pay + tax lines, tappable month rows)
    const val payTrendChartTitle = "Pay Trend"
    const val payTrendLegendNetPay = "Net Pay"
    const val payTrendLegendTax = "Income Tax"

    // Recommended Actions (contextual CTAs not already surfaced by a Smart Insights card)
    const val recommendedActionTaxPlannerTitle = "Tax Saving Opportunity"
    const val recommendedActionTaxPlannerDesc = "See your 80C/NPS headroom and estimated tax savings"
    const val recommendedActionClaimGeneratorTitle = "Draft a Representation"
    const val recommendedActionClaimGeneratorDesc = "One or more findings on this payslip qualify for a PCDA(O) claim"
    const val recommendedActionDsopSimulatorTitle = "Grow Your DSOP Corpus"
    const val recommendedActionDsopSimulatorDesc = "You have monthly DSOP headroom — simulate the retirement impact"
    const val recommendedActionRetirementCalcTitle = "Plan Your Retirement"
    const val recommendedActionRetirementCalcDesc = "Estimate pension, gratuity, commutation & leave encashment"

    // Premium Report (compact AI audit entry + expandable premium tools list)
    const val premiumReportTitle = "Premium Report"
    const val premiumReportViewAllToolsLabel = "View all premium tools"
    const val premiumReportHideToolsLabel = "Hide premium tools"
    const val premiumReportToolsExpandDesc = "Expand premium tools"
    const val premiumReportToolsCollapseDesc = "Collapse premium tools"

    // Locked PRO hub (consolidates the Recommended Actions / Advanced Anomalies / Premium Report
    // teasers into one card for free users — see docs/plans "Insights tab" consolidation)
    const val premiumHubTitle = "Unlock PayslipMax PRO"
    const val premiumHubCta = "Unlock Everything"
    const val premiumHubBundleIntro = "One subscription unlocks:"
    const val premiumHubMostRelevantPrefix = "★ Most relevant for you: "
}
