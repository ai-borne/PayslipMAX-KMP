package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry

/**
 * Reads the "Leave Accumulation Details" table from a statement's full text. PDFBox and PDFKit both emit it one row per
 * line, in both printed layouts:
 * - Dec 2023 onward: `31/12/2024 19 301 ACCLVE 381 02/05/2025`
 * - Dec 2022: `14 31.12.2020 12 192 ACCLVE 0446/2021 06.05.2021 0` (serial number first, trailing "Leave" column)
 *
 * Fails closed: a malformed row, a table cut by a page break, or an oversized table gives null, never a partial table,
 * because the balance feeds a retirement-encashment figure. Plain string scanning only, with no regex, so it stays linear
 * on Kotlin/Native (see LeaveAccumulationIosPerfTest).
 */
object LeaveAccumulationExtractor {
    const val MAX_ROWS = 200

    private const val HEADING = "accumulation details"
    private const val COLUMN_HEADER = "cas date"
    private const val HEADER_LOOKAHEAD = 3
    private const val MAX_SERIAL_DIGITS = 3
    private const val MAX_DAY_DIGITS = 4
    private const val MAX_ORDER_NO_LENGTH = 12
    private const val MIN_CODE_LENGTH = 3
    private const val MAX_CODE_LENGTH = 10
    private const val DATE_LENGTH = 10
    private const val MIN_YEAR = 1950
    private const val MAX_YEAR = 2100

    fun extract(fullText: String): LeaveAccumulation? {
        val lines = fullText.lines()
        val headingIndex = lines.indexOfFirst { it.contains(HEADING, ignoreCase = true) }
        if (headingIndex < 0) return null
        val headerIndex =
            (headingIndex + 1..minOf(headingIndex + HEADER_LOOKAHEAD, lines.lastIndex))
                .firstOrNull { lines[it].contains(COLUMN_HEADER, ignoreCase = true) } ?: return null

        val entries = mutableListOf<LeaveEntry>()
        var index = headerIndex + 1
        while (index <= lines.lastIndex) {
            val tokens = tokensOf(lines[index])
            if (!startsLikeRow(tokens)) break
            entries += parseRow(tokens) ?: return null
            if (entries.size > MAX_ROWS) return null
            index++
        }
        if (entries.isEmpty()) return null
        // A complete row after the block ends means the table ran on past a page break: refuse the truncated part.
        if ((index..lines.lastIndex).any { parseRow(tokensOf(lines[it])) != null }) return null
        return LeaveAccumulation(entries)
    }

    private fun tokensOf(line: String): List<String> = line.split(' ', '\t', ' ').filter { it.isNotEmpty() }

    /** Shape only, so a row with an impossible date still counts as a row and fails the section instead of ending it. */
    private fun startsLikeRow(tokens: List<String>): Boolean =
        tokens.firstOrNull()?.let(::hasDateShape) == true ||
            (tokens.size > 1 && isSerial(tokens[0]) && hasDateShape(tokens[1]))

    private fun parseRow(tokens: List<String>): LeaveEntry? {
        val first = tokens.firstOrNull() ?: return null
        val start =
            when {
                hasDateShape(first) -> 0
                isSerial(first) -> 1
                else -> return null
            }
        val casDate = tokens.getOrNull(start)?.let(::isoDate) ?: return null
        val codeIndex = (start + 1 until tokens.size).firstOrNull { isCasCode(tokens[it]) } ?: return null
        val numbers = (start + 1 until codeIndex).map { dayCount(tokens[it]) ?: return null }
        val orderNo = tokens.getOrNull(codeIndex + 1)?.takeIf(::isOrderNo) ?: return null
        val orderDate = tokens.getOrNull(codeIndex + 2)?.let(::isoDate) ?: return null
        val trailing = tokens.size - (codeIndex + 3)
        if (trailing > 1 || (trailing == 1 && dayCount(tokens.last()) == null)) return null

        val code = tokens[codeIndex]
        val (days, total) = daysAndTotal(code, numbers) ?: return null
        return LeaveEntry(casDate, code, days, total, orderNo, orderDate)
    }

    /** An LTC row's last number is the days encashed (the Accu AL column before it is blank or 0); an accrual needs both. */
    private fun daysAndTotal(
        code: String,
        numbers: List<Int>,
    ): Pair<Int, Int?>? =
        when {
            code == LeaveCasCode.LTC_ENCASHED -> if (numbers.size in 1..2) numbers.last() to null else null
            numbers.size == 2 -> numbers[0] to numbers[1]
            code != LeaveCasCode.ACCRUED && numbers.size == 1 -> numbers[0] to null
            else -> null
        }

    private fun isSerial(token: String): Boolean = token.length in 1..MAX_SERIAL_DIGITS && token.all(Char::isDigit)

    private fun dayCount(token: String): Int? = if (token.length in 1..MAX_DAY_DIGITS && token.all(Char::isDigit)) token.toInt() else null

    private fun isCasCode(token: String): Boolean = token.length in MIN_CODE_LENGTH..MAX_CODE_LENGTH && token.all { it in 'A'..'Z' }

    private fun isOrderNo(token: String): Boolean =
        token.length in 1..MAX_ORDER_NO_LENGTH && token.any(Char::isDigit) && token.all { it.isDigit() || it == '/' }

    /** `dd/mm/yyyy` or `dd.mm.yyyy`, digits checked but not the calendar. */
    private fun hasDateShape(token: String): Boolean {
        if (token.length != DATE_LENGTH) return false
        val separator = token[2]
        if ((separator != '/' && separator != '.') || token[5] != separator) return false
        return token.withIndex().all { (i, c) -> i == 2 || i == 5 || c.isDigit() }
    }

    private fun isoDate(token: String): String? {
        if (!hasDateShape(token)) return null
        val day = token.substring(0, 2).toInt()
        val month = token.substring(3, 5).toInt()
        val year = token.substring(6, 10).toInt()
        if (year !in MIN_YEAR..MAX_YEAR || month !in 1..12 || day !in 1..daysIn(month, year)) return null
        return "${token.substring(6, 10)}-${token.substring(3, 5)}-${token.substring(0, 2)}"
    }

    private fun daysIn(
        month: Int,
        year: Int,
    ): Int =
        when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
}
