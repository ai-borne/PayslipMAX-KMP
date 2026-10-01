package com.payslipmax.pdfparser.ui.theme

/** Copy for the Pay Audit entry points (docs/Plan Phase 3): dashboard banner, "Audit this month" CTA, orientation sheet. */
object PayAuditEntryStrings {
    const val dashboardBannerTitle = "Pay Audit"
    const val dashboardBannerCta = "Open Pay Audit"

    fun dashboardBannerSubtitle(payslipCount: Int): String =
        "Check your $payslipCount ${if (payslipCount == 1) "payslip" else "payslips"} against the rules"

    const val replicaCtaTitle = "Audit this month"
    const val replicaCtaBody = "See whether each pay line on this payslip is correct"

    const val orientationTitle = "How Pay Audit works"
    const val orientationGotIt = "Got it"
    const val orientationGlossaryHint = "Tap ⓘ on the audit screen any time to see what DNI, Stage and TPTA mean."
    val orientationPoints =
        listOf(
            "Built from your payslips" to
                "Your level, increments, DA steps and postings are rebuilt from the payslips you have uploaded. You type nothing.",
            "Only proven issues" to
                "An issue appears only when your payslips prove it, with the pay line, the amount and the rule behind it. " +
                "If a later payslip is needed to be sure, it is shown as waiting.",
            "Every change explained" to
                "Each month-to-month change in a pay line comes with its reason, so you can see why your pay moved.",
        )
}
