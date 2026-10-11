package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.guide.preclaim.PreClaimIssue

/**
 * One plain sentence for every reason a pre-claim element has no estimate. `when` is exhaustive on purpose: a new [PreClaimIssue] in
 * `shared` does not compile here until it is worded. Each sentence says what to fix, and none repeats a value the officer typed.
 */
object PreClaimIssueStrings {
    fun of(issue: PreClaimIssue): String =
        when (issue) {
            PreClaimIssue.FIGURE_MISSING -> "The rate for this is not in the Guide this version of the app carries."
            PreClaimIssue.CASE_NOT_IN_FIGURE -> "The Guide has no rate for this case."
            PreClaimIssue.BASIC_MISSING -> "Enter your basic pay."
            PreClaimIssue.BASIC_OUT_OF_RANGE -> "Check your basic pay: it looks too high or too low."
            PreClaimIssue.LEVEL_MISSING -> "Choose your pay level."
            PreClaimIssue.LEVEL_NOT_COVERED -> "The Guide has no rate for this pay level."
            PreClaimIssue.DA_MISSING -> "Enter the dearness allowance percentage."
            PreClaimIssue.DA_OUT_OF_RANGE -> "Check the dearness allowance percentage: it looks too high."
            PreClaimIssue.KM_MISSING -> "Enter the road distance in kilometres."
            PreClaimIssue.KM_OUT_OF_RANGE -> "Check the road distance: it looks too long or too short."
            PreClaimIssue.STATE_MISSING -> "Choose the state the journey starts in."
            PreClaimIssue.STATE_NOT_LISTED -> "The rate table does not list this state."
            PreClaimIssue.DATES_MISSING -> "Enter both dates."
            PreClaimIssue.DATE_INVALID -> "One of the dates does not exist. Check the day and month."
            PreClaimIssue.DATES_REVERSED -> "The to date is before the from date."
            PreClaimIssue.DAYS_OUT_OF_RANGE -> "Check the dates: the stay looks too long."
            PreClaimIssue.MINUTES_MISSING -> "Enter how long you are away, in hours and minutes."
            PreClaimIssue.MINUTES_OUT_OF_RANGE -> "Check the time away: it must be within one day."
            PreClaimIssue.NIGHTS_MISSING -> "Enter the number of nights in a hotel."
            PreClaimIssue.NIGHTS_OUT_OF_RANGE -> "Check the nights: they cannot be more than the days away."
            PreClaimIssue.NIGHTLY_RATE_MISSING -> "Enter your hotel bill for one night."
            PreClaimIssue.NIGHTLY_RATE_OUT_OF_RANGE -> "Check the hotel bill: it looks too high or too low."
            PreClaimIssue.TR_RULE_MISSING -> "Choose the TR Rule."
            PreClaimIssue.SANCTION_UNANSWERED -> "Say whether you hold the Rule 40 Note 2 sanction."
            PreClaimIssue.BOTH_SPOUSES_SERVING -> "With both spouses serving, the transfer grant is not worked out here. Ask your pay office how it applies to you."
        }
}
