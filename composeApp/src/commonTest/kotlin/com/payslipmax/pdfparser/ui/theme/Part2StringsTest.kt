package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.DraftIssueReason
import com.payslipmax.pdfparser.part2.domain.LinkBasis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun theTwoAckChecksSayTheManualRejectsTheOrderAndCiteWhatTheyCheck() {
        // why: Round 11 made these two the checks the officer must tick, because the manual says the order will be rejected.
        assertTrue("60 days" in Part2Strings.finding("al_year_cap")!! && "rejected" in Part2Strings.finding("al_year_cap")!!)
        assertTrue("rejected" in Part2Strings.finding("cl_combination")!!)
        assertEquals("ODP-2026, p 115, note 2", Part2Strings.citation(LinkBasis.MANUAL, 115, "2"))
        assertEquals("ODP-2026, p 117", Part2Strings.citation(LinkBasis.MANUAL, 117, null))
        assertEquals("Seen in practice", Part2Strings.citation(LinkBasis.PRACTICE, null, null), "practice is never passed off as the manual, whatever page it carries")
        assertEquals("Seen in practice", Part2Strings.citation(LinkBasis.PRACTICE, 49, "ii"))
    }

    @Test
    fun anIdWithNoWordingIsNotMistakenForAQuestionAndNoRuleIsWordedTwice() {
        assertTrue(Part2Strings.hasQuestion("ration") && Part2Strings.hasQuestion("rh_code"))
        assertFalse(Part2Strings.hasQuestion("no_such_question"))
        assertNull(Part2Strings.finding("no_such_rule"))
    }

    @Test
    fun everyReasonAnAnswerCanBeRefusedForHasWordsAndNamesItsField() {
        for (reason in DraftIssueReason.entries) assertTrue(Part2Strings.issue(reason).isNotBlank(), "$reason")
        assertEquals("Last day is before the first day", Part2Strings.fieldIssue("Last day", DraftIssueReason.DATES_REVERSED))
    }

    @Test
    fun anUnfinishedDraftSaysSoAndWhatIsStillNeededWhenKnown() {
        assertEquals("Not finished", Part2Strings.unfinishedLine(emptyList()))
        assertEquals("Not finished. Still needed: First day, Year", Part2Strings.unfinishedLine(listOf("First day", "Year")))
    }
}
