package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.AcclveCalculator
import com.payslipmax.pdfparser.part2.domain.FillNote
import com.payslipmax.pdfparser.part2.domain.YearCheck
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The words of the Leave Bank fill (plan, phase 8b). They state the officer's numbers back and nothing else, say plainly when nothing was
 * changed, and take the 30, 60 and 300 from the one calculator so no second copy of a cap can drift. Synthetic figures.
 */
class Part2LeaveBankStringsTest {
    @Test
    fun theYearsWithNoLineAreListedAsRunsSoALongGapIsOneShortPhrase() {
        assertEquals("2009", Part2LeaveBankStrings.years(listOf(2009)))
        assertEquals("2011 and 2012", Part2LeaveBankStrings.years(listOf(2011, 2012)))
        assertEquals("2009 and 2011 to 2013", Part2LeaveBankStrings.years(listOf(2009, 2011, 2012, 2013)))
        assertEquals("2009 to 2011 and 2015", Part2LeaveBankStrings.years(listOf(2009, 2010, 2011, 2015)))
        assertEquals("2008, 2010 and 2012", Part2LeaveBankStrings.years(listOf(2008, 2010, 2012)))
        assertEquals("2009 to 2020", Part2LeaveBankStrings.years((2009..2020).toList()))
    }

    @Test
    fun theAccrualNoteStatesTheLeaveUsedTheDaysAllowedAndTheManualsCapsFromTheCalculator() {
        val note = Part2LeaveBankStrings.note(FillNote.AccrualLimit(used = 45, allowed = 15))
        assertTrue("45 days" in note && "15 days" in note, note)
        assertTrue("${AcclveCalculator.ACCRUAL_CAP_DAYS} a year" in note && "${AcclveCalculator.YEAR_USE_CAP_DAYS} together" in note, note)
        assertTrue("p.134" in note && "notes 5 and 6" in note, note)
    }

    @Test
    fun theBreakUpNotesNameTheMissingYearsAndBothTotalsAndPromiseNothingWasChanged() {
        val missing = Part2LeaveBankStrings.note(FillNote.MissingYears(listOf(2009, 2011, 2012, 2013)))
        assertTrue("2009 and 2011 to 2013" in missing, missing)
        val total = Part2LeaveBankStrings.note(FillNote.StatementTotal(statement = 301, breakup = 61))
        assertTrue("301" in total && "61" in total && "Nothing was changed" in total, total)
    }

    @Test
    fun eachCrossCheckResultNamesTheYearAndTheFiguresAndADifferenceChangesNothing() {
        assertEquals("Matches your Leave Bank: the statement shows 291 days for 2024.", Part2LeaveBankStrings.crossCheck(YearCheck(2024, CrossCheck.Matches(291))))
        val differs = Part2LeaveBankStrings.crossCheck(YearCheck(2024, CrossCheck.Differs(drafted = 292, statement = 291)))
        assertTrue("2024" in differs && "292" in differs && "291" in differs, differs)
        assertTrue("Nothing was changed" in differs && "before you print" in differs, differs)
        assertEquals("Not checked: your Leave Bank has no row for 2025 yet.", Part2LeaveBankStrings.crossCheck(YearCheck(2025, CrossCheck.NoStatement)))
    }

    @Test
    fun withNoLeaveBankStatementTheFillMessageNamesOnlyTheSourceItUsed() {
        // Item 69(a): the officer with no statement was told figures came from "your Leave Bank", which they do not have.
        assertEquals("Filled 2 figures from your printed drafts. Check each one before you go on.", Part2LeaveBankStrings.filled(2, fromLeaveBank = false))
        assertEquals("Filled 1 figure from your printed drafts. Check it before you go on.", Part2LeaveBankStrings.filled(1, fromLeaveBank = false))
        assertEquals("Filled 2 figures from your Leave Bank and your printed drafts. Check each one before you go on.", Part2LeaveBankStrings.filled(2, fromLeaveBank = true))
        assertEquals(Part2LeaveBankStrings.filled(3, fromLeaveBank = true), Part2LeaveBankStrings.filled(3), "a statement is the usual case")
    }

    @Test
    fun theFillMessagesSayWhatHappenedInPlainWordsAndPluralsAreRight() {
        assertTrue(Part2LeaveBankStrings.filled(1).startsWith("Filled 1 figure from"), Part2LeaveBankStrings.filled(1))
        assertTrue(Part2LeaveBankStrings.filled(4).startsWith("Filled 4 figures from"), Part2LeaveBankStrings.filled(4))
        assertTrue(Part2LeaveBankStrings.filled(4).contains("Check each one"))
        assertTrue(Part2LeaveBankStrings.filled(1).contains("Check it"))
        for (text in listOf(Part2LeaveBankStrings.unchanged, Part2LeaveBankStrings.needsYear, Part2LeaveBankStrings.nothingKnown, Part2LeaveBankStrings.waiting)) assertTrue(text.isNotBlank() && text.endsWith("."), text)
        assertTrue("year" in Part2LeaveBankStrings.needsYear)
    }

    @Test
    fun theTwoButtonsDifferAndTheRefillQuestionOffersAReplaceAndAKeep() {
        assertEquals("Fill from your Leave Bank", Part2LeaveBankStrings.fillButton)
        assertEquals("Refill from your Leave Bank", Part2LeaveBankStrings.refillButton)
        assertTrue(Part2LeaveBankStrings.refillBody.isNotBlank() && Part2LeaveBankStrings.refillConfirm != Part2LeaveBankStrings.refillKeep)
    }
}
