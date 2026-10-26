package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.AcclveRules
import com.payslipmax.pdfparser.part2.domain.EncashRules
import com.payslipmax.pdfparser.part2.domain.LeaveRules
import com.payslipmax.pdfparser.part2.domain.LongLeaveRules
import com.payslipmax.pdfparser.part2.domain.LtcRules
import com.payslipmax.pdfparser.part2.domain.RationRules

/**
 * Copy for the rest of Chapter 5 (docs/Plan/12_Part2Orders_PhasePlan.md, Phase 8; ODP-2026 pp.99, 115-141): what its checks say and how the
 * form words the questions that feed them. Kept apart from [Part2Strings] and [Part2CancelStrings] so none of them nears the 300-line
 * limit. Where the manual says an order "will be rejected" the wording says so, because those are the checks the officer must tick.
 */
object Part2LongLeaveStrings {
    private const val QUOTE_LATER = "once it is published. Until then it prints blank."

    /** What a check of this batch says, by the rule's id; null for an id that is not this batch's. */
    fun finding(ruleId: String): String? =
        when (ruleId) {
            LongLeaveRules.FURLOUGH_CYCLE -> "A furlough cycle is three calendar years, and the leave must fall inside it. Check the years."
            LongLeaveRules.FURLOUGH_NOT_NCC -> "Furlough is not granted to NCC officers or re-employed officers."
            LongLeaveRules.CCL_LIMITS -> "Child care leave is 5 to 30 days a spell and at most 60 days in a calendar year."
            LongLeaveRules.TERMINAL_LEAVE_28 -> "Terminal leave is 28 days. The dates give another number once any annual leave beside it is taken out."
            LongLeaveRules.TERMINAL_WITH_ANNUAL -> "Beside annual leave, terminal leave starts on the first day of the annual leave and ends on the day the terminal leave expires."
            LongLeaveRules.ADOPTION_LEAVE_180 -> "Child adoption leave is 180 days. The dates do not give 180 days."
            LongLeaveRules.PATERNITY_ADOPTION_15 -> "Paternity leave on adoption is 15 days. The dates do not give 15 days."
            LongLeaveRules.GRANT_QUOTED_FURLOUGH -> "Quote the order that granted the furlough $QUOTE_LATER"
            LongLeaveRules.GRANT_QUOTED_STUDY -> "Quote the order that granted the study leave $QUOTE_LATER"
            LongLeaveRules.GRANT_QUOTED_OVERSTAY -> "Quote the order that granted the leave you overstayed $QUOTE_LATER"
            LongLeaveRules.DECLARED_QUOTED_OVERSTAY -> "Quote the order that declared the overstayal $QUOTE_LATER"
            LongLeaveRules.DECLARED_QUOTED_AWL -> "Quote the order that declared the absence $QUOTE_LATER"
            LongLeaveRules.REISSUE_ALLOWANCES ->
                "An order cannot be amended. The ration or Risk and Hardship rows of the earlier order ran to the old return day, so they are cancelled and notified afresh."
            AcclveRules.ACCRUAL_30 -> "Leave accumulated for a year may not be more than 30 days."
            AcclveRules.YEAR_60 -> "The year's accumulation and the annual leave used in it together may not be more than 60 days."
            AcclveRules.TOO_EARLY -> "This accumulation is published on or after 1 March of the next year, or on retirement. Today is earlier than that."
            AcclveRules.OVER_CAP -> "The total is above 300 days. Only 300 days can be encashed."
            LtcRules.DAYS_OVER_10 -> "Annual leave is encashed ten days at a time on LTC."
            LtcRules.TOTAL_OVER_60 -> "Annual leave encashed on LTC is at most 60 days in a whole career."
            LtcRules.CREDIT_UNDER_30 -> "At least 30 days of leave must stay at credit after the encashment."
            LtcRules.ADVANCE_QUOTED -> "Quote the order that granted the advance $QUOTE_LATER"
            EncashRules.YEAR_OVER_30 -> "Leave accumulated in a year should not be more than 30 days. Check the figure for that year."
            EncashRules.OVER_CAP -> "The break-up comes to more than 300 days. Only 300 days can be encashed, so the slip says 300."
            RationRules.RATION_LONG_LEAVE -> "Leave of 180 days or more: the unit publishes the ration orders quarterly in arrears, so none is drafted here."
            else -> null
        }

    /** The wording of a form question of this batch, by the id its rule family gave it; the id itself for any other. */
    fun question(id: String): String =
        when (id) {
            LeaveRules.ANSWER_NCC_REEMPLOYED -> "Are you an NCC officer or a re-employed officer?"
            LeaveRules.ANSWER_REEMPLOYED_SAME_YEAR -> "Were you retired and re-employed in the same calendar year?"
            LongLeaveRules.ANSWER_EARLIER_ALLOWANCES -> "Did the earlier order also grant or stop ration or Risk and Hardship allowance?"
            AcclveRules.ANSWER_RETIRING -> "Is this accumulation published on your retirement?"
            else -> id
        }
}
