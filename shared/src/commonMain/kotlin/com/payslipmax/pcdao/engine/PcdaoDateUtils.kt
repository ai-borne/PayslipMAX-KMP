package com.payslipmax.pcdao.engine

internal object PcdaoDateUtils {
    fun parseDate(dateStr: String): Triple<Int, Int, Int> {
        val parts = dateStr.trim().split("-")
        require(parts.size == 3) { "Invalid date format: '$dateStr'. Expected YYYY-MM-DD" }
        val year = parts[0].toIntOrNull() ?: error("Invalid year in '$dateStr'")
        val month = parts[1].toIntOrNull() ?: error("Invalid month in '$dateStr'")
        val day = parts[2].toIntOrNull() ?: error("Invalid day in '$dateStr'")
        return Triple(year, month, day)
    }

    fun dateToEpochDays(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        var y = year.toLong()
        var m = month.toLong()
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val era = y / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun daysBetween(
        startDate: String,
        endDate: String,
    ): Int {
        val (y1, m1, d1) = parseDate(startDate)
        val (y2, m2, d2) = parseDate(endDate)
        val epoch1 = dateToEpochDays(y1, m1, d1)
        val epoch2 = dateToEpochDays(y2, m2, d2)
        return (epoch2 - epoch1).toInt()
    }
}
