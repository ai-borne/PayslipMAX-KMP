package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.DraftIssueReason
import com.payslipmax.pdfparser.part2.domain.LinkBasis
import com.payslipmax.pdfparser.part2.domain.Part2Rules
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
    fun theChecksOfTheRestOfChapterFiveAreWordedAndTheAckOnesSayTheManualRejectsTheOrder() {
        // why: the manual says "will be rejected" only of the 60-day cap and casual leave beside another leave; no new check may claim it.
        val shipped = Part2Rules.shipped().ids.filter { Part2Strings.finding(it) != null }
        assertEquals(setOf("al_year_cap", "cl_combination"), shipped.filter { "rejected" in Part2Strings.finding(it)!! }.toSet())
        assertTrue("not granted" in Part2Strings.finding("furlough_not_ncc")!! && "5 to 30 days" in Part2Strings.finding("ccl_limits")!!)
        assertTrue("1 March" in Part2Strings.finding("acclve_too_early")!! && "300" in Part2Strings.finding("acclve_over_cap")!!)
        assertTrue("180 days" in Part2Strings.finding("ration_long_leave")!! && "quarterly" in Part2Strings.finding("ration_long_leave")!!)
        assertTrue("cannot be amended" in Part2Strings.finding("reissue_allowances")!!)
        for (id in listOf("grant_quoted_furlough", "grant_quoted_study", "grant_quoted_overstay", "declared_quoted_overstay", "declared_quoted_awl")) {
            assertTrue("prints blank" in Part2Strings.finding(id)!!, id)
        }
        assertTrue("any other kind of leave" in Part2Strings.finding("cl_combination")!!)
    }

    @Test
    fun theNewFormQuestionsAreWordedAndAnUnknownOneIsNot() {
        for (id in listOf("ncc_reemployed", "reemployed_same_year", "earlier_allowances", "acclve_retiring")) assertTrue(Part2Strings.hasQuestion(id), id)
        assertEquals("Are you an NCC officer or a re-employed officer?", Part2Strings.question("ncc_reemployed"))
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

    @Test
    fun theJobNameIsFixedAndNamesNoCodeDateOrNumberBecauseItShowsInThePrintQueue() {
        // why: the name is the system's to keep (the queue, the saved PDF's name), so nothing about a draft may be in it (Round 18).
        assertEquals("Part II slip", Part2Strings.printJobName)
        assertTrue(Part2Strings.printJobName.none { it.isDigit() })
    }

    @Test
    fun aCertificatePrintsAsTheManualWordsItWithNoLabelInFront() {
        // why: every certificate in the manual starts with the word CERTIFICATE (Round 18), so a label of ours would read it twice.
        assertEquals("CERTIFICATE : CLAIMING RISK AND HARDSHIP ALLOWANCE Certified that:-", Part2Strings.certificateLine("CERTIFICATE : CLAIMING RISK AND HARDSHIP ALLOWANCE Certified that:-"))
    }

    @Test
    fun thePageLabelIsATemplateTheLayoutFillsIn() {
        assertEquals("Page {page} of {pages}", Part2Strings.pageLabel)
    }

    @Test
    fun thePrintedAndCheckLinesNameTheirFigures() {
        assertEquals("Printed on 05/07/2026", Part2Strings.printedOnLine("05/07/2026"))
        assertEquals("Row 2 of 7", Part2Strings.hrmsRowHeading(2, 7))
        assertTrue("template" in Part2Strings.templateUpdatedSincePrinted && "printed" in Part2Strings.templateUpdatedSincePrinted)
        assertTrue("Changed" in Part2Strings.changedSincePrinted)
    }
}
