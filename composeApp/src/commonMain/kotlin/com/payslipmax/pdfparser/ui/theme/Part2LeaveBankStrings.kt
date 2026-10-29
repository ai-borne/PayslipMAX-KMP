package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.AcclveCalculator
import com.payslipmax.pdfparser.part2.domain.FillNote
import com.payslipmax.pdfparser.part2.domain.YearCheck
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck

/**
 * Copy for filling the year-end forms from the Leave Bank and for the check against its statement (docs/Plan/12_Part2Orders_PhasePlan.md,
 * Phase 8b). Kept apart from [Part2Strings] (near its limit). The caps are the calculator's own numbers, never retyped here. The words state
 * the officer's figures back and say plainly when nothing was changed.
 */
object Part2LeaveBankStrings {
    const val fillButton = "Fill from your Leave Bank"
    const val refillButton = "Refill from your Leave Bank"
    const val refillTitle = "Replace these figures?"
    const val refillBody = "The figures from your Leave Bank and your printed drafts will replace the ones in this form. Boxes they do not fill are left as they are."
    const val refillConfirm = "Replace"
    const val refillKeep = "Keep mine"
    const val crossCheckHeading = "Leave Bank check"
    const val waiting = "Waiting for your Leave Bank."
    const val unchanged = "Nothing changed. These figures are already the ones your Leave Bank gives."
    const val needsYear = "Type the leave year first, then fill."
    const val nothingKnown = "Your Leave Bank has nothing to fill this from. Import a December payslip that prints the leave table, or type the figures."

    /** What a fill that changed [count] boxes says; without a statement ([fromLeaveBank] false) it does not claim a Leave Bank gave them. */
    fun filled(
        count: Int,
        fromLeaveBank: Boolean = true,
    ): String {
        val source = if (fromLeaveBank) "your Leave Bank and your printed drafts" else "your printed drafts"
        return if (count == 1) "Filled 1 figure from $source. Check it before you go on." else "Filled $count figures from $source. Check each one before you go on."
    }

    /** The fact a form note states, in words. */
    fun note(note: FillNote): String =
        when (note) {
            is FillNote.AccrualLimit ->
                "Annual leave used in the year: ${note.used} days. At most ${note.allowed} days can be accumulated: ${AcclveCalculator.ACCRUAL_CAP_DAYS} a year, " +
                    "and ${AcclveCalculator.YEAR_USE_CAP_DAYS} together with the leave used (ODP-2026 p.134, notes 5 and 6)."
            is FillNote.MissingYears -> "No line for ${years(note.years)}. Add a line for any of these years in which you accumulated leave."
            is FillNote.StatementTotal -> "Your Leave Bank statement shows a total of ${note.statement} days, but these lines add up to ${note.breakup}. Nothing was changed."
        }

    /** The line under the slip that compares the drafted total with the statement. */
    fun crossCheck(check: YearCheck): String =
        when (val result = check.result) {
            is CrossCheck.Matches -> "Matches your Leave Bank: the statement shows ${result.total} days for ${check.year}."
            is CrossCheck.Differs ->
                "Your total for ${check.year} is ${result.drafted} days but your Leave Bank statement shows ${result.statement} days. " +
                    "Nothing was changed. Check both figures before you print."
            CrossCheck.NoStatement -> "Not checked: your Leave Bank has no row for ${check.year} yet."
        }

    /** Years in a short phrase: three or more in a row as "a to b", the rest one by one, "and" before the last. */
    fun years(years: List<Int>): String {
        val parts = mutableListOf<String>()
        var start = 0
        while (start < years.size) {
            var end = start
            while (end + 1 < years.size && years[end + 1] == years[end] + 1) end++
            if (end - start >= RUN_OF_THREE) parts += "${years[start]} to ${years[end]}" else (start..end).forEach { parts += years[it].toString() }
            start = end + 1
        }
        return if (parts.size <= 1) parts.joinToString() else parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }

    private const val RUN_OF_THREE = 2
}
