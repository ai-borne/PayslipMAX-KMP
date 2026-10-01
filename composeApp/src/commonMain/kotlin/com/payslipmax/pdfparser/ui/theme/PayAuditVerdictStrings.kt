package com.payslipmax.pdfparser.ui.theme

/** Pay Audit redesign copy (docs/Plan/09 Phase 2): verdict, month picker, tabs, evidence cards, glossary. */
object PayAuditVerdictStrings {
    const val verdictLabelCorrect = "✓ Correct"
    const val verdictLabelIssue = "! Issue found"
    const val verdictLabelWaiting = "… Waiting"
    const val verdictLabelNoPayslip = "No payslip"

    const val cleanHeadlineMiddle = ": no issues found on "
    const val cleanHeadlineSuffix = " pay lines"
    const val cleanSubtitle = "Nothing to claim this month."
    const val cleanVerifiedPrefix = "Arrears credits verified, they match the rules exactly: "
    const val issueShortSuffix = " short"
    const val issueCountSingular = "1 issue, proven from your own payslips."
    const val issueCountPluralSuffix = " issues, proven from your own payslips."
    const val lockedHeadlinePrefix = "Issue: "
    const val lockedSubtitle = "Unlock Pay Audit to see the amount and the evidence."
    const val lockedCta = "Unlock the amount"
    const val waitingHeadlineSuffix = " waiting for your next payslip"
    const val waitingSubtitle = "Arrears are not credited yet. They normally arrive 1–3 months later. Not counted as an issue."
    const val noPayslipHeadline = "No payslip for this month"
    const val noPayslipSubtitle = "Pick a month that has a payslip, or add this one from History."
    const val seeWhyAndDraftCta = "See why and draft a letter"
    const val seeEvidenceCta = "See the evidence"

    const val historyMonthsSuffix = " months audited · "
    const val historyIssuesSingular = "1 issue"
    const val historyIssuesPluralSuffix = " issues"
    const val historyZeroIssues = "0 issues"

    const val previousMonthDescription = "Previous month"
    const val nextMonthDescription = "Next month"
    const val monthPickerTitle = "Pick a month"
    const val monthPickerNote = "Months without a payslip are greyed out."
    const val monthPickerNoPayslip = "no payslip"
    const val infoGlyph = "ⓘ"
    const val dropdownSuffix = " ▾"

    const val tabThisMonth = "This month"
    const val tabHistory = "History"
    const val tabPlanAhead = "Plan ahead"

    const val kickerIssue = "Issue · Proven"
    const val kickerWaiting = "Waiting"
    const val kickerVerified = "Verified · not an issue"
    const val kickerLocked = "Issue · locked"
    const val evidenceShouldBe = "Should be"
    const val evidenceCredited = "Credited"
    const val evidenceDifference = "Difference"
    const val whyButton = "Why?"
    const val hideWhyButton = "Hide"
    const val whyAuthorityPrefix = "Authority: "
    const val whyNoAuthority = "This is based on your own earlier payslips."
    const val draftLetterButton = "Draft letter"
    const val waitingNote = "We check again when your next payslip is added. If it is still missing then, it becomes an issue."
    const val lockedCardNote = "Amount, evidence and the letter are part of Premium. The verdict above and the changes below stay free."
    const val lockedCardCta = "Unlock evidence and letter"

    const val changesTitle = "What changed from last month"
    const val changesEmpty = "No pay line changed this month."
    const val spansTitle = "Service timeline"
    const val everyChangeTitle = "Every change, by year"
    const val changesCountSingular = " change"
    const val changesCountPlural = " changes"

    const val spanStageSeparator = " · Stage "
    const val spanMonthsSuffix = " months"
    const val spanFirstPaidPrefix = "First paid on the "
    const val spanFirstPaidSuffix = " payslip"
    const val spanRiskHardshipPosting = "Risk & Hardship posting"
    const val spanFieldPosting = "Field posting"

    const val glossaryTitle = "What these terms mean"
    const val glossaryClose = "Close"
    val glossary: List<Pair<String, String>> =
        listOf(
            "Stage" to "Your annual increment step within a pay level. Stage 8 means 8 increments earned at this level.",
            "DNI" to "Date of Next Increment. The month your basic pay next steps up.",
            "TPTA" to "Transport Allowance, paid at a higher rate in listed cities. DA is added on top.",
            "MSP" to "Military Service Pay, a fixed monthly amount for officers at Level 14 and above.",
            "DA" to "Dearness Allowance. A percentage of basic pay that changes on 1 Jan and 1 Jul.",
            "Waiting" to "The payslip looks short but arrears normally follow later. It is not counted as an issue.",
            "Verified" to "An arrears credit that matches the rules exactly. Shown for your records, never counted as an issue.",
        )

    /** Plain-words names for the pay-line field keys shared by the auditors and the change explainer. */
    val payLineLabels: Map<String, String> =
        mapOf(
            "basicPay" to "Basic pay",
            "dearnessAllowance" to "Dearness Allowance (DA)",
            "arrearsDa" to "DA arrears",
            "arrearsTptaDa" to "TPTA DA arrears",
            "arrearsTpta" to "TPTA arrears",
            "militaryServicePay" to "Military Service Pay (MSP)",
            "transportAllowance" to "TPTA (transport allowance)",
            "houseRentAllowance" to "House rent allowance",
            "licenseFee" to "Licence fee",
            "riskHardshipAllowance" to "Risk & Hardship allowance",
            "fieldAllowance" to "Field allowance",
            "nonPracticingAllowance" to "Non-practising allowance",
        )
}
