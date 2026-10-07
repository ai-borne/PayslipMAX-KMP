package com.payslipmax.pdfparser.guide.domain

/**
 * When the "rates may have changed" nudge shows. Dearness allowance is revised twice a year, so rates dated
 * [STALE_AFTER_MONTHS] or more months before today have very likely been overtaken. The clock is passed in as epoch
 * milliseconds (never read here), and the month is taken in UTC; a day's difference at a month end cannot matter at
 * this scale. Plain arithmetic, no date library and no regex.
 */
object GuideStaleness {
    /** Owner-reviewable (see plan EP 13): three quarters of a year, so one DA revision has always been announced. */
    const val STALE_AFTER_MONTHS = 9

    private const val MILLIS_PER_DAY = 86_400_000L

    /** True when [ratesAsOf] ("YYYY-MM") is [STALE_AFTER_MONTHS] or more months before [nowMillis]; false if malformed. */
    fun isStale(
        ratesAsOf: String,
        nowMillis: Long,
    ): Boolean {
        val (ratesYear, ratesMonth) = parseYearMonth(ratesAsOf) ?: return false
        val (nowYear, nowMonth) = yearMonthOf(nowMillis)
        return (nowYear * 12 + nowMonth) - (ratesYear * 12 + ratesMonth) >= STALE_AFTER_MONTHS
    }

    /** The (year, month 1-12) of an instant in UTC. */
    fun yearMonthOf(epochMillis: Long): Pair<Int, Int> {
        // Civil-from-days (Howard Hinnant): days since 1970-01-01 to a proleptic Gregorian date.
        val z = epochMillis.floorDiv(MILLIS_PER_DAY) + 719_468
        val era = z.floorDiv(146_097L)
        val dayOfEra = z - era * 146_097
        val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthShifted = (5 * dayOfYear + 2) / 153
        val month = (if (monthShifted < 10) monthShifted + 3 else monthShifted - 9).toInt()
        val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
        return year to month
    }

    /** Days since 1970-01-01 of a date; the inverse of [yearMonthOf], so tests can build a fixed clock value. */
    fun epochDays(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val y = if (month <= 2) year - 1 else year
        val era = y.floorDiv(400)
        val yearOfEra = y - era * 400
        val monthShifted = if (month > 2) month - 3 else month + 9
        val dayOfYear = (153 * monthShifted + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097L + dayOfEra - 719_468
    }

    private fun parseYearMonth(value: String): Pair<Int, Int>? {
        if (value.length != 7 || value[4] != '-') return null
        val year = value.substring(0, 4).toIntOrNull() ?: return null
        val month = value.substring(5).toIntOrNull() ?: return null
        return if (month in 1..12) year to month else null
    }
}
