package com.payslipmax.pdfparser.domain

import kotlinx.serialization.Serializable

/**
 * The "Leave Accumulation Details" table a PCDA(O) statement prints (December statements, Dec 2022 onward), in printed
 * order. Figures are PCDA's own and are never recomputed: the running totals don't always add up row to row, and the
 * printed total is the record. Read from the latest statement only; earlier statements can list different rows.
 */
@Serializable
data class LeaveAccumulation(
    val entries: List<LeaveEntry>,
)

/**
 * One Part II order row. For an accrual ([LeaveCasCode.ACCRUED]) [days] is the leave accumulated that year and
 * [totalDays] the printed running total. For an LTC encashment ([LeaveCasCode.LTC_ENCASHED]) [days] is the days encashed
 * and [totalDays] is null. Dates are ISO `yyyy-MM-dd`; [casCode] and [orderNo] are kept exactly as printed.
 */
@Serializable
data class LeaveEntry(
    val casDate: String,
    val casCode: String,
    val days: Int,
    val totalDays: Int? = null,
    val orderNo: String,
    val orderDate: String,
)

/** The cas codes the app gives a meaning to. Any other code is kept as printed. */
object LeaveCasCode {
    const val ACCRUED = "ACCLVE"
    const val LTC_ENCASHED = "LVELTC"
}
