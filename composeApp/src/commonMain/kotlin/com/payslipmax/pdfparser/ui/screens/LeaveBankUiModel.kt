package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.insights.LeaveBankSummary
import com.payslipmax.pdfparser.insights.RetirementCalculatorEngine
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings

/** The Leave bank card's text, all of it from [LeaveBankSummary]: nothing here recomputes a figure the summary holds. */
data class LeaveBankCardUi(
    val balance: String,
    val overCap: String?,
    val asOf: String,
    val ltc: String,
    val worth: String,
    val worthBasis: String,
    val ordersTitle: String,
    val rows: List<LeaveRowUi>,
)

/** One printed leave row; [order] is the Part II order shown when the row is tapped. */
data class LeaveRowUi(
    val date: String,
    val type: String,
    val days: String,
    val total: String,
    val order: String,
)

fun leaveBankCardUi(summary: LeaveBankSummary): LeaveBankCardUi =
    LeaveBankCardUi(
        balance = LeaveBankStrings.days(summary.balanceDays),
        overCap = summary.daysOverCap.takeIf { it > 0 }?.let(LeaveBankStrings::overCap),
        asOf = LeaveBankStrings.asOf(leaveDisplayDate(summary.asOfDate), summary.statementMonth()),
        ltc = LeaveBankStrings.ltcUsed(summary.ltcDaysUsed, summary.ltcCapDays),
        worth = LeaveBankStrings.worth(formatCurrency(summary.valueAtCurrentPay)),
        worthBasis = LeaveBankStrings.worthBasis(formatPayMonth(PayMonth(summary.payYear, summary.payMonth)), summary.encashableDays),
        ordersTitle = LeaveBankStrings.ordersTitle(summary.entries.size),
        rows = summary.entries.map(::leaveRowUi),
    )

/** The leave-days input starts at what can be encashed from the latest statement, or PCDA's 300-day cap without one. */
fun initialLeaveDays(summary: LeaveBankSummary?): String = (summary?.encashableDays ?: RetirementCalculatorEngine.LEAVE_MAX_DAYS).toString()

fun leaveDaysSourceLabel(summary: LeaveBankSummary): String = LeaveBankStrings.leaveDaysSource(summary.statementMonth(), summary.balanceDays)

/** ISO `yyyy-MM-dd` as "31 Dec 2025". Anything else is shown as stored: a date the app can't read is never guessed at. */
fun leaveDisplayDate(iso: String): String {
    val parts = iso.split('-')
    val year = parts.getOrNull(0)?.takeIf { it.length == YEAR_LENGTH }?.toIntOrNull()
    val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..MONTHS_IN_YEAR }
    val day = parts.getOrNull(2)?.toIntOrNull()?.takeIf { it in 1..MAX_DAY_OF_MONTH }
    return if (parts.size == ISO_PARTS && year != null && month != null && day != null) "$day ${formatPayMonth(PayMonth(year, month))}" else iso
}

private fun leaveRowUi(entry: LeaveEntry) =
    LeaveRowUi(
        date = leaveDisplayDate(entry.casDate),
        type =
            when (entry.casCode) {
                LeaveCasCode.ACCRUED -> LeaveBankStrings.typeAccrued
                LeaveCasCode.LTC_ENCASHED -> LeaveBankStrings.typeLtcEncashed
                else -> entry.casCode
            },
        days = entry.days.toString(),
        total = entry.totalDays?.toString() ?: LeaveBankStrings.noTotal,
        order = LeaveBankStrings.order(entry.orderNo, leaveDisplayDate(entry.orderDate)),
    )

private fun LeaveBankSummary.statementMonth(): String = formatPayMonth(PayMonth(sourceYear, sourceMonth))

private const val ISO_PARTS = 3
private const val YEAR_LENGTH = 4
private const val MONTHS_IN_YEAR = 12
private const val MAX_DAY_OF_MONTH = 31
