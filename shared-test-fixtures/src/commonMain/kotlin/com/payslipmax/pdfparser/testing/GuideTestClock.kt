package com.payslipmax.pdfparser.testing

/** Fixed clock values for tests, so no test reads the device clock. */
object GuideTestClock {
    /** Days since 1970-01-01 of a date; the inverse of `GuideStaleness.yearMonthOf`. */
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
}
