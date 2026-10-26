package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.section
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the Chapter 5 batch ships in the real bundle (Part II plan, phase 8), apart from ACCLVE and the LTC codes: the 17 serials 5.6 to
 * 5.22, each on the page the lookup gives, in group A, reachable from the Leave ribbon with an offer to draft, within the shipped limits
 * and carrying no markup. The ribbon's drafting offer comes from the layout, so no screen names a code.
 */
class Part2BatchThreeShipTest {
    private val batch = listOf("FURLEV", "MTRNLV", "SPMTRNLV", "GTDCCL", "EOL", "TERLEV", "CHADPLVE", "PTRNLV", "PATLCHAD", "STDLEV", "STDLVAL", "STDLVFUR", "EXTNFUR", "OSL", "REJOSL", "AWL", "REJAWL")

    private val templates get() = section.templates.filter { it.code in batch }

    @Test
    fun theSeventeenSerialsAreAllShippedOnceEach() {
        assertEquals(batch.sorted(), templates.map { it.code }.sorted())
    }

    @Test
    fun eachTemplateCitesThePageTheLookupGivesAndSitsInGroupA() {
        val page = section.serials.flatMap { p -> p.codes.map { it to p } }.toMap()
        for (t in templates) {
            assertEquals(page.getValue(t.code).page, t.page, t.code)
            assertEquals("A", page.getValue(t.code).group, t.code)
            assertTrue(page.getValue(t.code).serial.startsWith("5."), t.code)
        }
    }

    @Test
    fun theLeaveRibbonOffersToDraftEveryCodeOfTheBatch() {
        // why: the manual's own cases group these codes; the officer finds them there and by name in search.
        val index = Part2LookupIndex(section)
        val offered = listOf("leave_furlough", "leave_family", "leave_late").flatMap { id -> index.case(id)!!.rows.map { it.draftCode } }
        assertEquals(batch.sorted(), offered.filterNotNull().filter { it in batch }.sorted())
        assertTrue(offered.all { it != null }, "no row of these three cases is lookup-only")
    }

    @Test
    fun noShippedWordIsOverTheShippedLimitsAndNoneCarriesMarkup() {
        val limits = checkNotNull(section.templateLimits)
        for (t in templates) {
            assertTrue(listOfNotNull(t.narrative, t.authority, t.certificate).all { it.isNotBlank() && it.length <= limits.narrative && '<' !in it && '>' !in it }, t.code)
            assertTrue(t.slots.all { it.label.length <= limits.label && it.help.length <= limits.help }, t.code)
            assertTrue(t.slots.size <= limits.slots && t.links.size <= limits.links && t.rules.size <= limits.rules, t.code)
        }
    }

    @Test
    fun everyRationLinkIsCitedToTheManualAndOnlyTheLeavesTheManualNamesCarryThem() {
        // why: the ration four are manual-backed (p.98 note 1.8, p.99 note 5); a link on any other leave would be a guess presented as a rule.
        val carrying = templates.filter { it.links.isNotEmpty() }.map { it.code }
        assertEquals(listOf("FURLEV", "GTDCCL", "MTRNLV", "SPMTRNLV"), carrying.sorted())
        for (t in templates.filter { it.links.isNotEmpty() }) {
            assertEquals(listOf("SORS", "GTDRSHNA", "TORS", "STPRSHNA"), t.links.map { it.code }, t.code)
            assertTrue(t.links.all { it.basis == "manual" && it.condition == RationRules.ON_RATION_UNDER_180 }, t.code)
        }
    }

    @Test
    fun noChapterFiveLeaveOtherThanAnnualLeaveCarriesAnR2hLink() {
        // why: Round 11 and Round 30 - R&H follows annual leave only; the manual never says leave stops it.
        for (t in templates) assertTrue(t.links.none { it.code.startsWith("GTDRH") || it.code == "STPRH" }, t.code)
    }
}
