package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.crypto.CryptoHelper

data class SalaryCountdownUiModel(
    val daysRemaining: Int,
    val paydayDateFormatted: String,
    val isPaydayToday: Boolean,
    val progressRatio: Float,
    val currentDay: Int,
    val totalDaysInMonth: Int,
)

object SalaryCountdownCalculator {
    private val monthNames =
        listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun getDaysInMonth(
        year: Int,
        month: Int,
    ): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    fun calculateCountdown(
        year: Int,
        month: Int,
        day: Int,
    ): SalaryCountdownUiModel {
        val totalDays = getDaysInMonth(year, month)
        val validDay = day.coerceIn(1, totalDays)
        val daysRemaining = (totalDays - validDay).coerceAtLeast(0)
        val monthStr = if (month in 1..12) monthNames[month - 1] else ""
        val paydayDateFormatted = "$totalDays $monthStr".trim()
        val isPaydayToday = daysRemaining == 0
        val progressRatio = (validDay.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)

        return SalaryCountdownUiModel(
            daysRemaining = daysRemaining,
            paydayDateFormatted = paydayDateFormatted,
            isPaydayToday = isPaydayToday,
            progressRatio = progressRatio,
            currentDay = validDay,
            totalDaysInMonth = totalDays,
        )
    }

    /**
     * Converts epoch milliseconds to (year, month 1-12, day 1-31) in UTC/GMT.
     */
    fun epochMillisToDate(epochMillis: Long): Triple<Int, Int, Int> {
        val totalDays = (epochMillis / (24 * 60 * 60 * 1000L)).toInt()
        return epochDayToDate(totalDays)
    }

    fun getCurrentCountdown(): SalaryCountdownUiModel {
        val nowMs = CryptoHelper.getCurrentTimeMillis()
        val (year, month, day) = epochMillisToDate(nowMs)
        return calculateCountdown(year, month, day)
    }

    private fun epochDayToDate(epochDay: Int): Triple<Int, Int, Int> {
        // Civil date computation from epoch day (0 = 1970-01-01)
        var z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        var y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        if (m <= 2) y += 1
        return Triple(y, m, d)
    }
}
