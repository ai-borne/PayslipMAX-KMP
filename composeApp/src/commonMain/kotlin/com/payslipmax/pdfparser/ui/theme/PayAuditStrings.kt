package com.payslipmax.pdfparser.ui.theme

/** Pay Audit screen copy (docs/Plan/09_PayAudit_PhasePlan.md Phase 4) — kept out of the near-full [AppStrings]. */
object PayAuditStrings {
    const val screenTitle = "Pay Audit"
    const val screenSubtitle = "Every rupee, checked against the rules"
    const val emptyState = "Upload a payslip to start your Pay Audit."

    const val entryCardSubtitle = "Every rupee of your pay, checked against the rules"
    const val entryCardNoFindings = "No findings on this payslip"

    const val timelineSectionTitle = "Service Timeline"
    const val timelineEmptyState = "Upload more payslips to build your service timeline."
    const val timelineLevelPrefix = "Level "
    const val timelineStageSeparator = ", Stage "
    const val timelineDaSuffix = "% DA"
    const val timelineLevelUnresolved = "Level unresolved"
    const val timelineQuartersLabel = " · Quarters"
    const val timelineTptaHigherLabel = " · TPTA (higher-rate city)"

    const val changesSectionTitle = "What changed this month"
    const val changesEmptyState = "No pay-line changes are explained for this month yet."

    const val findingsSectionTitle = "Findings"
    const val findingsEmptyState = "No findings on this payslip — everything checks out."
    const val findingsCountSingular = "finding found on this payslip"
    const val findingsCountPlural = "findings found on this payslip"
    const val findingsLockedCta = "Unlock Pay Audit findings"
    const val findingsExpectedLabel = "Expected: ₹"
    const val findingsActualLabel = "Actual: ₹"
    const val findingsAuthorityLabel = "Authority: "

    const val predictionsSectionTitle = "What's next"
    const val nextIncrementTitle = "Next increment"
    const val nextIncrementEmptyState = "Upload more payslips to predict your next increment."
    const val nextIncrementDatePrefix = "Due "
    const val nextIncrementOverduePrefix = "Overdue since "
    const val nextIncrementAmountPrefix = "Basic Pay moves to ₹"
    const val dsopRoomTitle = "DSOP room this year"
    const val dsopRoomSubscribedPrefix = "Subscribed so far: ₹"
    const val dsopRoomLeftPrefix = "Room left under the ₹5L tax-free cap: ₹"

    const val fixationCalculatorTitle = "Pay-fixation calculator"
    const val fixationCalculatorSubtitle = "Compare Option 1 (fixed from promotion) vs Option 2 (fixed from your next DNI)."
    const val fixationCalculatorEmptyState = "Upload more payslips to use the pay-fixation calculator."
    const val fixationCalculatorToLevelLabel = "Promoted to Level"
    const val fixationCalculatorMonthLabel = "Promotion month"
    const val fixationCalculatorYearLabel = "Promotion year"
    const val fixationCalculatorOption1Label = "Option 1 — fixed pay"
    const val fixationCalculatorOption2Label = "Option 2 — fixed pay"
    const val fixationCalculatorRecommendedPrefix = "Recommended: Option "
    const val fixationCalculatorNextIncrementPrefix = "Next increment: "
    const val fixationCalculatorInvalidInput = "Enter a valid promotion year and a level higher than your current one."
    const val fixationCalculatorDniAssumedNote =
        "No increment found in your payslips yet — assuming a January DNI cycle. Actual cycle month may differ."
}
