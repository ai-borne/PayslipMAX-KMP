package com.payslipmax.pdfparser.part2.leavebank

import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.domain.YearDays
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Leave Bank rows as the year-end orders' figures (Part II plan, phase 8). The rows follow what a real December statement prints: the
 * accumulation of a year is dated 31 December of it and appears on the statement of the next year, an LTC encashment is dated the day it was
 * paid, and the running total is the statement's own (so 301 can stand beside 282 + 19). All figures are synthetic.
 */
class LeaveBankFiguresTest {
    private val rows =
        listOf(
            accrued("2022-12-31", 30, 252),
            ltcEncashed("2023-05-01", 5),
            accrued("2023-12-31", 30, 282),
            ltcEncashed("2024-04-26", 10),
            accrued("2024-12-31", 19, 301),
        )

    @Test
    fun thePreviousTotalIsTheStatementsRowOfTheYearBeforeAndTheLtcDaysAreThoseDatedInTheYear() {
        val suggestion = LeaveBankFigures.suggest(2024, rows, earlierDraftTotal = 999, alUsed = 20)
        assertEquals(282, suggestion.previousTotal, "the row dated 31/12/2023, not the draft")
        assertEquals(10, suggestion.ltcDays)
        assertEquals(30, suggestion.accrued, "min(30, 60 - 20)")
        assertEquals(20, suggestion.alUsed)
        assertEquals(5, LeaveBankFigures.suggest(2023, rows, null, 0).ltcDays, "only the row dated in 2023, and 2024 above counts only its own 10")
    }

    @Test
    fun withNoStatementRowTheLastDraftsTotalIsTheFallbackAndOtherwiseNothingIsGuessed() {
        assertEquals(282, LeaveBankFigures.suggest(2024, emptyList(), earlierDraftTotal = 282, alUsed = 0).previousTotal)
        assertNull(LeaveBankFigures.suggest(2024, emptyList(), earlierDraftTotal = null, alUsed = 0).previousTotal)
        assertNull(LeaveBankFigures.suggest(2030, rows, null, 0).previousTotal, "no row of 2029")
        assertEquals(0, LeaveBankFigures.suggest(2024, emptyList(), null, 0).ltcDays)
    }

    @Test
    fun theNewestRowOfAYearWinsWhenTwoShareIt() {
        val two = listOf(accrued("2023-12-31", 10, 100, orderDate = "2024-03-01"), accrued("2023-12-31", 30, 282, orderDate = "2024-05-01"))
        assertEquals(282, LeaveBankFigures.suggest(2024, two, null, 0).previousTotal)
    }

    @Test
    fun aRowWhoseDateIsNotAnIsoDateIsIgnoredNeverGuessedAt() {
        val odd = listOf(LeaveEntry("31/12/2023", "ACCLVE", 30, 282, "1", "31/12/2023"), accrued("20x3-12-31", 5, 5), accrued("2023/12/31", 9, 9), accrued("2023-12-31", 30, 282))
        assertEquals(282, LeaveBankFigures.suggest(2024, odd, null, 0).previousTotal)
        assertNull(LeaveBankFigures.suggest(2024, odd.take(3), null, 0).previousTotal)
        assertEquals(listOf(YearDays(2023, 30)), LeaveBankFigures.breakup(odd))
    }

    @Test
    fun theDraftedTotalIsComparedWithTheStatementOfTheYearAfterAndNothingIsChangedByIt() {
        assertEquals(CrossCheck.Matches(301), LeaveBankFigures.crossCheck(2024, 301, rows))
        assertEquals(CrossCheck.Differs(drafted = 302, statement = 301), LeaveBankFigures.crossCheck(2024, 302, rows), "both figures are shown")
        assertEquals(CrossCheck.NoStatement, LeaveBankFigures.crossCheck(2025, 301, rows), "the statement of the next year is not held yet")
        assertEquals(CrossCheck.NoStatement, LeaveBankFigures.crossCheck(2024, 301, emptyList()))
    }

    @Test
    fun theBreakupIsTheStatementsAccumulationYearByYearOldestFirstWithItsGapsNamed() {
        val statement = listOf(accrued("2011-12-31", 8, 20), accrued("2008-12-31", 7, 7), accrued("2010-12-31", 5, 12), ltcEncashed("2017-04-26", 10), accrued("2013-12-31", 9, 29))
        val breakup = LeaveBankFigures.breakup(statement)
        assertEquals(listOf(YearDays(2008, 7), YearDays(2010, 5), YearDays(2011, 8), YearDays(2013, 9)), breakup, "only accumulation rows, oldest first")
        assertEquals(listOf(2009, 2012), LeaveBankFigures.missingYears(breakup), "the years the statement has no row for")
        assertEquals(emptyList(), LeaveBankFigures.missingYears(listOf(YearDays(2008, 7))))
        assertEquals(emptyList(), LeaveBankFigures.missingYears(emptyList()))
        assertEquals(emptyList(), LeaveBankFigures.missingYears(listOf(YearDays(2010, 5), YearDays(2011, 8))))
    }

    @Test
    fun aRowTheBreakupCouldNotHoldIsLeftOutAndAHugeStatementIsCutToTheNewest60() {
        assertEquals(emptyList(), LeaveBankFigures.breakup(listOf(accrued("1949-12-31", 5, 5), accrued("2101-12-31", 5, 5), accrued("2020-12-31", 10_000, 10_000))))
        val long = (0 until 70).map { accrued("${1950 + it}-12-31", 1, it + 1) }
        val cut = LeaveBankFigures.breakup(long)
        assertEquals(60, cut.size)
        assertEquals(1960, cut.first().year, "the oldest ten fall off")
        assertEquals(2019, cut.last().year)
    }

    @Test
    fun theStatementTotalIsTheRunningTotalOfTheNewestAccumulationRowAndNullWhenThereIsNone() {
        // why: the break-up of an encashment is compared with what the statement itself prints (a statement can show 301).
        assertEquals(301, LeaveBankFigures.statementTotal(rows))
        assertEquals(301, LeaveBankFigures.statementTotal(rows.reversed()), "by date, not by position")
        assertNull(LeaveBankFigures.statementTotal(listOf(ltcEncashed("2024-04-26", 10))), "an LTC row is not an accumulation")
        assertEquals(301, LeaveBankFigures.statementTotal(rows + ltcEncashed("2025-04-26", 3)), "a later LTC row (no total) does not hide the accumulation before it")
        assertNull(LeaveBankFigures.statementTotal(emptyList()))
        assertNull(LeaveBankFigures.statementTotal(listOf(LeaveEntry("2024-12-31", LeaveCasCode.ACCRUED, 19, null, "1", "2025-01-01"))), "a row that prints no total")
    }
}
