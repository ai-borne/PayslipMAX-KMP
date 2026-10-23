package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the officer must find any of the 303 code pages, from the tiles or from a search, and must never be shown a tile for a ribbon
 * whose drafts do not ship yet (Round 8) or a group the manual never printed (Round 8). The index is pure, so these run on the
 * synthetic lookup; the real 303 are walked by `Part2BrowseContract`.
 */
class Part2LookupIndexTest {
    private val lookup = SyntheticPart2Lookup.lookup
    private val index = Part2LookupIndex(lookup)

    private fun serials(query: String): List<String> = assertIs<Part2SearchResult.Results>(index.search(query)).rows.map { it.serial }

    @Test
    fun tilesShowOnlyForBuiltRibbonsInBuildOrder() {
        assertEquals(listOf(Part2RibbonTile("leave", "The first ribbon", 2), Part2RibbonTile("records", "The second ribbon", 1)), index.ribbons())
        assertNull(index.ribbon("career"), "an unbuilt ribbon has no screen, even by id")
        assertNull(index.case("career_one"), "nor do its cases")
        assertNull(index.ribbon("nope"))
    }

    @Test
    fun aRibbonListsItsCasesWithTheChaptersAndPagesTheirCodePagesCover() {
        val leave = assertNotNull(index.ribbon("leave"))
        assertEquals(
            listOf(
                Part2CaseTile("leave_one", "First case", Part2Span(listOf(1), 28, 29), 2),
                Part2CaseTile("leave_two", "Second case", Part2Span(listOf(2, 3, 5), 39, 131), 3),
            ),
            leave.cases,
        )
    }

    @Test
    fun aCaseListsItsCodesMarksTheLookupOnlyOnesAndListsTheAlsoCodesApart() {
        val one = assertNotNull(index.case("leave_one"))
        assertEquals(listOf("1.1", "1.2"), one.rows.map { it.serial })
        assertEquals(listOf("3.1"), one.also.map { it.serial })
        assertEquals(listOf("Granted the first thing", "Reported for the second thing"), one.rows.map { it.name })
        val two = assertNotNull(index.case("leave_two"))
        assertEquals(listOf(false, true, false), two.rows.map { it.lookupOnly }, "only 3.1 is marked lookup-only")
        assertEquals(emptyList(), two.also)
    }

    @Test
    fun aVariantSerialIsOneRowWithAllItsCodesAndTheNoGroupSerialHasNoGroup() {
        val records = assertNotNull(index.case("records_one"))
        val variant = records.rows.first { it.serial == SyntheticPart2Lookup.VARIANT_SERIAL }
        assertEquals(listOf("VARIANT1", "VARIANT2"), variant.codes)
        assertEquals("B", variant.group)
        assertNull(records.rows.first { it.serial == SyntheticPart2Lookup.NO_GROUP_SERIAL }.group, "the manual prints no group: none is inferred")
    }

    @Test
    fun aCodePageCarriesTheMinimalFieldsAndTheGroupsMeaning() {
        val page = assertNotNull(index.code("FIRSTCODE"))
        assertEquals(
            Part2CodeContent("1.1", listOf("FIRSTCODE"), "Granted the first thing", Part2GroupInfo("A", "Money"), 1, 28, emptyList()),
            page,
        )
    }

    @Test
    fun anyCodeOfAVariantSerialOpensTheSameWholePage() {
        assertEquals(index.code("VARIANT1"), index.code("VARIANT2"))
        assertEquals(listOf("VARIANT1", "VARIANT2"), index.code("VARIANT2")!!.codes)
    }

    @Test
    fun aCodePageForASerialWithNoPrintedGroupHasNoGroupAtAll() {
        assertNull(assertNotNull(index.code("STOPCODE")).group, "the screen says 'Group not printed in ODP-2026'; the model holds nothing")
    }

    @Test
    fun rejoinalsPageSaysTheSlipPrintsRejoinAndNoOtherPageSaysAnything() {
        assertEquals(listOf(Part2PrintedAs(SyntheticPart2Templates.REJOIN_ALL, "REJOIN")), assertNotNull(index.code("REJOINAL")).printedAs)
        assertTrue(lookup.serials.filter { it.codes != listOf("REJOINAL") }.all { index.code(it.codes.first())!!.printedAs.isEmpty() })
    }

    @Test
    fun anUnbuiltRibbonsCodePageIsStillOpenAndAnUnknownOrLowercaseCodeIsNot() {
        assertNotNull(index.code(SyntheticPart2Lookup.LATER_CODE), "search reaches every code page (Round 8)")
        assertNull(index.code("NOSUCHCODE"))
        assertNull(index.code("firstcode"), "codes are the manual's capitals; a saved or typed lowercase id is not a page")
        assertNull(index.code(""))
    }

    @Test
    fun theKnowsChecksFollowTheSameRules() {
        assertTrue(lookup.knowsRibbon("leave"))
        assertFalse(lookup.knowsRibbon("career"), "unbuilt")
        assertFalse(lookup.knowsRibbon("nope"))
        assertTrue(lookup.knowsCase("records_one"))
        assertFalse(lookup.knowsCase("career_one"))
        assertTrue(lookup.knowsCode("VARIANT2"))
        assertTrue(lookup.knowsCode(SyntheticPart2Lookup.LATER_CODE))
        assertFalse(lookup.knowsCode("NOSUCHCODE"))
    }

    @Test
    fun searchFindsACodeExactlyByItsPrefixAndWithoutRegardToCase() {
        assertEquals(listOf("1.1"), serials("firstcode"))
        assertEquals(listOf("1.1"), serials("FIRSTC"))
        assertEquals(listOf("3.1"), serials("Fourth11"))
    }

    @Test
    fun searchByAVariantCodeOpensThatVariant() {
        val row = assertIs<Part2SearchResult.Results>(index.search("variant2")).rows.single()
        assertEquals(Triple("3.27", listOf("VARIANT1", "VARIANT2"), "VARIANT2"), Triple(row.serial, row.codes, row.openCode))
    }

    @Test
    fun searchByNameMatchesWordStartsAndNeedsEveryWord() {
        assertEquals(listOf("1.1", "3.1", "3.27", "4.1"), serials("granted thing"))
        assertEquals(listOf("3.1"), serials("granted fourth"))
        assertEquals(emptyList(), serials("granted second"), "every word must match somewhere")
        assertEquals(listOf("5.23"), serials("rejoin"), "a word start matches, and the code REJOINAL starts with it too")
    }

    @Test
    fun searchByTheSerialNumberFindsExactlyThatSerial() {
        assertEquals(listOf("3.28"), serials("3.28"))
        assertEquals(listOf("5.23"), serials(" 5.23 "))
        assertEquals(emptyList(), serials("3.2"), "a serial is matched whole, so 3.2 is not 3.27 or 3.28")
    }

    @Test
    fun searchReachesTheCodesOfAnUnbuiltRibbonToo() {
        assertEquals(listOf("4.1", "4.2"), serials("later"))
        assertTrue(index.ribbons().none { it.id == "career" })
    }

    @Test
    fun anExactCodeRanksAheadOfALongerCodeThatMerelyStartsWithIt() {
        val longer = Part2CodePage("9.1", 9, listOf("ABCDEF"), "A", "First invented", 150)
        val exact = Part2CodePage("9.2", 9, listOf("ABCD"), "A", "Second invented", 151)
        val crafted: Part2Lookup = lookup.copy(serials = lookup.serials + longer + exact)
        val rows = assertIs<Part2SearchResult.Results>(Part2LookupIndex(crafted).search("abcd")).rows
        assertEquals(listOf("9.2", "9.1"), rows.map { it.serial })
    }

    @Test
    fun aSerialIsMatchedWholeSoFivePointOneIsNotFivePointTen() {
        val crafted = lookup.copy(serials = lookup.serials + Part2CodePage("5.1", 5, listOf("AAAA1"), "A", "Alpha", 114) + Part2CodePage("5.10", 5, listOf("AAAA10"), "A", "Beta", 118))
        assertEquals(listOf("5.1"), (Part2LookupIndex(crafted).search("5.1") as Part2SearchResult.Results).rows.map { it.serial })
    }

    @Test
    fun aSerialShapedTokenThatIsNoSerialFindsNothingEvenWhenANameHasThoseDigits() {
        // why: real names carry numbers ("33)", "@ 24%"); "3.2" must not turn into the words 3 and 2 and list them.
        val numbered = Part2CodePage("9.1", 9, listOf("NUMBERED"), "A", "Granted 33 items for 2 lots", 150)
        val crafted = lookup.copy(serials = lookup.serials + numbered)
        assertEquals(Part2SearchResult.Results(emptyList()), Part2LookupIndex(crafted).search("3.2"))
        assertEquals(listOf("9.1"), (Part2LookupIndex(crafted).search("33 lots") as Part2SearchResult.Results).rows.map { it.serial }, "the same digits as words still find it")
    }

    @Test
    fun aCodeMatchRanksAheadOfANameMatch() {
        // "variant" is the start of 3.27's code and a word in both names; 3.28 (a name only) must come after it.
        assertEquals(listOf("3.27", "3.28"), serials("variant"))
    }

    @Test
    fun emptyAndOneLetterQueriesSearchNothingAndNoMatchIsAnEmptyList() {
        assertEquals(Part2SearchResult.Idle, index.search(""))
        assertEquals(Part2SearchResult.Idle, index.search("   "))
        assertEquals(Part2SearchResult.TooShort, index.search("a"))
        assertEquals(Part2SearchResult.Results(emptyList()), index.search("zzzzqq"))
    }

    @Test
    fun anAbsurdQueryIsHarmless() {
        assertEquals(Part2SearchResult.Results(emptyList()), index.search("x".repeat(5_000)))
        assertEquals(Part2SearchResult.Results(emptyList()), index.search("<script> ' OR 1=1 -- \u0000"))
    }

    @Test
    fun aLookupWithNothingInItIsEmptyNotACrash() {
        val empty = Part2LookupIndex(lookup.copy(serials = emptyList(), ribbons = emptyList(), templates = emptyList()))
        assertEquals(emptyList(), empty.ribbons())
        assertEquals(Part2SearchResult.Results(emptyList()), empty.search("anything"))
        assertNull(empty.code("FIRSTCODE"))
    }
}
