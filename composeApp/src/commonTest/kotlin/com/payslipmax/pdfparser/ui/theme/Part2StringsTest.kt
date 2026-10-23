package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: some Part II words are owner decisions or manual facts, not styling, so they are pinned by literal: a copy edit that changes
 * them must be a deliberate one. Everything else on the browse screens is chrome and is exercised by the screen tests.
 */
class Part2StringsTest {
    @Test
    fun theBlankGroupWordsAreRoundEightsExactCopy() {
        assertEquals("Group not printed in ODP-2026", Part2Strings.groupNotPrinted)
    }

    @Test
    fun theRejoinalNoteSaysWhatTheSlipPrintsAndWhatTheManualLists() {
        assertEquals("The slip prints REJOIN, as HRMS publishes it. ODP-2026 lists REJOINAL.", Part2Strings.printedAsNote("REJOINAL", "REJOIN"))
    }

    @Test
    fun aCaseSpanReadsAsChaptersAndPrintedPages() {
        assertEquals("Ch 5, p 114", Part2Strings.span(listOf(5), 114, 114))
        assertEquals("Ch 5, pp 114-117", Part2Strings.span(listOf(5), 114, 117))
        assertEquals("Ch 2, 3, 5, pp 39-131", Part2Strings.span(listOf(2, 3, 5), 39, 131))
    }

    @Test
    fun countsAndManualCitationsAreSingularWhereTheyShouldBe() {
        assertEquals("1 code", Part2Strings.codeCount(1))
        assertEquals("8 codes", Part2Strings.codeCount(8))
        assertEquals("Chapter 5, page 114 of ODP-2026", Part2Strings.chapterAndPage(5, 114))
        assertEquals("Group A", Part2Strings.groupLabel("A"))
    }
}
