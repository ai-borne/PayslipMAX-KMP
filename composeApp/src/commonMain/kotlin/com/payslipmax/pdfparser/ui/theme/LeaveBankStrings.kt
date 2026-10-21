package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase.LEAVE_MAX_DAYS

/**
 * Copy for every Leave Bank surface (docs/Plan/11_LeaveBank_PhasePlan.md). Wording agreed by the owner on 2026-10-21. LTC copy
 * states only the days used; it never says how LTC encashment affects retirement encashment (unsettled, see the plan).
 */
object LeaveBankStrings {
    const val cardTitle = "Leave bank"
    const val sectionTitle = "Leave accumulation"
    const val typeAccrued = "Accrued"
    const val typeLtcEncashed = "LTC encashed"
    const val columnDate = "Date"
    const val columnType = "Type"
    const val columnDays = "Days"
    const val columnTotal = "Total"
    const val noTotal = "—"

    fun days(count: Int): String = if (count == 1) "1 day" else "$count days"

    fun overCap(days: Int): String = "$days over cap"

    fun asOf(
        date: String,
        statementMonth: String,
    ): String = "As of $date · $statementMonth statement"

    fun ltcUsed(
        used: Int,
        cap: Int,
    ): String = "LTC: $used of $cap days used"

    fun worth(amount: String): String = "Worth ≈ $amount"

    fun worthBasis(
        payMonth: String,
        encashableDays: Int,
    ): String = "at $payMonth pay (${days(encashableDays)})"

    fun ordersTitle(count: Int): String = "Leave orders ($count)"

    fun order(
        orderNo: String,
        orderDate: String,
    ): String = "Order $orderNo · $orderDate"

    fun leaveDaysSource(
        statementMonth: String,
        printedDays: Int,
    ): String = "From your $statementMonth statement (${days(printedDays)} printed)"

    fun sectionSubtitle(
        asOfDate: String,
        balanceDays: Int,
    ): String = "As of $asOfDate · ${days(balanceDays)}"

    fun teaser(amount: String): String = "Your leave bank today ≈ $amount"

    const val capInsightTitle = "Leave at the $LEAVE_MAX_DAYS-day cap"
    const val capInsightAction = "See leave bank"

    /** The lapse fact only (owner decision 2026-10-21): nothing about LTC or how much is paid at retirement. */
    fun capInsightBody(
        statementMonth: String,
        balanceDays: Int,
        daysOverCap: Int,
    ): String {
        val position = if (daysOverCap > 0) "$daysOverCap over the cap" else "the cap"
        return "Your $statementMonth statement shows ${days(balanceDays)}, $position. Leave accrued beyond $LEAVE_MAX_DAYS days lapses."
    }
}
