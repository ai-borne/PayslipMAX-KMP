package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CLAIM
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NOTE
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NO_TEMPLATE
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.REJOIN_ALL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.STOP
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.UNGROUPED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Which codes the officer can start a draft with: those a built case homes that have a template, never the lookup-only or the "also see" ones. */
class Part2DraftOptionsTest {
    private val index = Part2LookupIndex(SyntheticPart2Templates.lookup)

    @Test
    fun theOptionsAreTheTemplateCodesOfBuiltCasesInTheManualsOrder() {
        assertEquals(listOf(GRANT, EXTENSION, RATION, STOP, CLAIM, NOTE, REJOIN_ALL), index.draftOptions().map { it.code })
    }

    @Test
    fun anOptionCarriesTheManualsNameAndNoCodeWithoutATemplateOrAHiddenOneIsOffered() {
        assertEquals("Invented thing $GRANT", index.draftOptions().first().name)
        val codes = index.draftOptions().map { it.code }
        assertEquals(false, NO_TEMPLATE in codes, "no template, nothing to draft")
        assertEquals(false, UNGROUPED in codes, "a lookup-only serial is never drafted")
    }

    @Test
    fun aCaseRowOffersItsCodeToDraftWhereItsSerialHasATemplate() {
        val rows = index.case("leave_two")!!.rows.associate { it.serial to it.draftCode }
        assertEquals(CLAIM, rows["2.1"])
        assertEquals(REJOIN_ALL, rows["3.1"])
        assertNull(rows["3.28"], "lookup-only")
        assertNull(rows["4.1"], "no template")
    }

    @Test
    fun anAlsoSeeRowAndASearchRowOfferNothingToDraft() {
        val lookup = SyntheticPart2Templates.lookup
        val withAlso = lookup.copy(ribbons = lookup.ribbons.map { r -> r.copy(cases = r.cases.map { if (it.id == "leave_two") it.copy(also = listOf("1.1")) else it }) })
        assertNull(Part2LookupIndex(withAlso).case("leave_two")!!.also.single().draftCode)
        val found = (index.search("granted") as Part2SearchResult.Results).rows
        assertEquals(emptyList(), found.filter { it.draftCode != null })
    }

    @Test
    fun aLookupWithNoTemplatesOffersNothing() {
        val bare = SyntheticPart2Templates.lookup.copy(templates = emptyList())
        assertEquals(emptyList(), Part2LookupIndex(bare).draftOptions())
    }
}
