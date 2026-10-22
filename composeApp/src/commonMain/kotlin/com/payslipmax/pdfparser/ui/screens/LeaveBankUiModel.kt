package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
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

/**
 * The payslip view's leave section. [subtitle] and [rows] are that payslip's own statement; [teaser] is today's leave bank from
 * the summary (latest statement, latest pay), so viewing an older December never prices or labels it with old figures.
 */
data class LeaveSectionUi(
    val subtitle: String?,
    val rows: List<LeaveRowUi>,
    val teaser: String?,
)

/** The table the payslip view shows: the one this payslip printed, and only while the kill switch is on. */
fun payslipLeaveTable(
    payslip: ParsedPayslip,
    enabled: Boolean = isLeaveBankEnabled(),
): LeaveAccumulation? = payslip.leaveAccumulation?.takeIf { enabled }

/** No teaser without a summary (no value to show) or while editing (leaving the payslip would drop the drafts). */
fun leaveSectionUi(
    table: LeaveAccumulation,
    summary: LeaveBankSummary?,
    isEditing: Boolean,
): LeaveSectionUi {
    val accrual = LeaveBankCalculator.latestAccrual(table.entries)
    return LeaveSectionUi(
        subtitle = accrual?.totalDays?.let { LeaveBankStrings.sectionSubtitle(leaveDisplayDate(accrual.casDate), it) },
        rows = table.entries.map(::leaveRowUi),
        teaser = summary?.takeUnless { isEditing }?.let { LeaveBankStrings.teaser(formatCurrency(it.valueAtCurrentPay)) },
    )
}

/** The leave-days input starts at what can be encashed from the latest statement, or PCDA's 300-day cap without one. */
fun initialLeaveDays(summary: LeaveBankSummary?): String = (summary?.encashableDays ?: RetirementCalculatorEngine.LEAVE_MAX_DAYS).toString()

fun leaveDaysSourceLabel(summary: LeaveBankSummary): String = LeaveBankStrings.leaveDaysSource(summary.statementMonth(), summary.balanceDays)

/**
 * The line under Retirement Calculators' leave field. Below the cap it adds that leave keeps accruing; with no summary it says
 * the cap is assumed, unless the feature is off ([enabled] false), where the field is the plain 300-day default and says nothing.
 */
fun leaveDaysNote(
    summary: LeaveBankSummary?,
    enabled: Boolean,
): String? =
    when {
        summary == null -> LeaveBankStrings.noStatementAssumesCap.takeIf { enabled }
        summary.balanceDays < RetirementCalculatorEngine.LEAVE_MAX_DAYS -> "${leaveDaysSourceLabel(summary)}. ${LeaveBankStrings.keepsAccruing()}"
        else -> leaveDaysSourceLabel(summary)
    }

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

internal fun LeaveBankSummary.statementMonth(): String = formatPayMonth(PayMonth(sourceYear, sourceMonth))

private const val ISO_PARTS = 3
private const val YEAR_LENGTH = 4
private const val MONTHS_IN_YEAR = 12
private const val MAX_DAY_OF_MONTH = 31
