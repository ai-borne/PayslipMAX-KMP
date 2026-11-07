package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2ColumnRow
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RelatedLink
import com.payslipmax.pdfparser.guide.model.Part2RelatedRow
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the officer must find any of the 303 casualties, from the ten ribbons or from a search, must see every ribbon (Learn is a wiki of
 * all of them, built or not), and must never be shown a group the manual never printed. A ribbon is one flat list under its case
 * names, so a case is a heading, not a place to go. The index is pure, so these run on the synthetic lookup; the real 303 are walked
 * by `Part2BrowseContract`.
 */
class Part2LookupIndexTest {
    private val lookup = SyntheticGuidePart2.lookup
    private val index = Part2LookupIndex(lookup)

    private fun serials(query: String): List<String> = assertIs<Part2SearchResult.Results>(index.search(query)).rows.map { it.serial }

    @Test
    fun everyRibbonShowsInBuildOrderWhetherOrNotItsOrdersCanBeDraftedAndCountsItsCasualties() {
        assertEquals(listOf(Part2RibbonTile("leave", "The first ribbon", 4), Part2RibbonTile("records", "The second ribbon", 2)), index.ribbons())
        assertNull(index.ribbon("nope"))
        assertTrue(lookup.knowsRibbon("records"), "Learn covers all ten ribbons, so an unbuilt one is a place")
        assertFalse(lookup.knowsRibbon("nope"))
    }

    @Test
    fun aRibbonIsOneFlatListWithTheCaseNamesAsHeadingsAndEachCasualtyOnce() {
        val leave = assertNotNull(index.ribbon("leave"))
        assertEquals(listOf("First case", "Second case"), leave.sections.map { it.title })
        assertEquals(listOf(listOf("1.1", "1.2"), listOf("2.1", "3.1")), leave.sections.map { s -> s.rows.map { it.serial } }, "3.1 is also-seen under the first case but homed once")
        val records = assertNotNull(index.ribbon("records"))
        assertEquals(listOf("3.27", "3.28"), records.sections.single().rows.map { it.serial }, "1.1 is homed in leave and is not repeated here")
        val all = index.ribbons().flatMap { tile -> assertNotNull(index.ribbon(tile.id)).sections.flatMap { s -> s.rows.map { it.serial } } }
        assertEquals(lookup.serials.map { it.serial }.sorted(), all.sorted())
    }

    @Test
    fun aRibbonRowCarriesTheCodesNameAndGroupAndAVariantIsOneRowWithAllItsCodes() {
        val rows = assertNotNull(index.ribbon("records")).sections.single().rows
        val variant = rows.first { it.serial == SyntheticGuidePart2.VARIANT_SERIAL }
        assertEquals(listOf("VARIANT1", "VARIANT2"), variant.codes)
        assertEquals("B", variant.group)
        assertNull(rows.first { it.serial == SyntheticGuidePart2.NO_GROUP_SERIAL }.group, "the manual prints no group: none is inferred")
    }

    @Test
    fun aCasualtyPageCarriesTheManualsFactsAndOnlyTheColumnsAnOrderUses() {
        val page = assertNotNull(index.code("FIRSTCODE"))
        assertEquals(Triple("1.1", "Granted the first thing", Part2GroupInfo("A", "Money")), Triple(page.serial, page.name, page.group))
        assertEquals(1 to 28, page.chapter to page.page)
        assertEquals(
            listOf(
                Part2ColumnLine(Part2ColumnPosition.FROM, Part2Need.REQUIRED, "dd/mm/yyyy"),
                Part2ColumnLine(Part2ColumnPosition.DATA_FIRST, Part2Need.OPTIONAL, "prefix"),
                Part2ColumnLine(Part2ColumnPosition.DATA_FIRST + 1, Part2Need.REQUIRED, "name"),
            ),
            page.columns,
            "To and Data 3 and 4 are none, so they have no line; positions say which column",
        )
    }

    @Test
    fun aRequiredColumnTheManualPrintsNoCaptionForKeepsAnEmptyCaptionForTheScreenToExplain() {
        val bare = Part2ColumnRow("1.1", Part2Column("required", ""), Part2Column("none", ""), List(4) { Part2Column("none", "") })
        val crafted = lookup.copy(columns = listOf(bare))
        assertEquals(listOf(Part2ColumnLine(Part2ColumnPosition.FROM, Part2Need.REQUIRED, "")), assertNotNull(Part2LookupIndex(crafted).code("FIRSTCODE")).columns)
    }

    @Test
    fun aSerialWithNoColumnRowHasNoColumnsNotACrash() {
        val crafted = lookup.copy(columns = emptyList())
        assertEquals(emptyList(), assertNotNull(Part2LookupIndex(crafted).code("FIRSTCODE")).columns)
    }

    @Test
    fun thePlainEnglishPageBelongsToEverySerialItCoversAndOthersHaveNone() {
        assertEquals(Part2AboutContent("What it is.", "When you see it.", "What to keep ready.", "One thing to watch."), assertNotNull(index.code("FIRSTCODE")).about)
        val shared = Part2AboutContent("A shared page.", "When.", "Ready.", "Watch.")
        assertEquals(shared, assertNotNull(index.code("FOURTH11")).about)
        assertEquals(shared, assertNotNull(index.code("VARIANT2")).about, "a page shared by sibling serials serves each of them")
        assertNull(assertNotNull(index.code("SECONDCODE")).about, "no page was written: the manual's data is all the page shows")
    }

    @Test
    fun usuallyPublishedWithListsEachCompanionWithItsNameAndWhyAndOnlyWhereDataExists() {
        assertEquals(
            listOf(
                Part2RelatedLine("SECONDCODE", "Reported for the second thing", Part2LinkBasis.MANUAL, 29),
                Part2RelatedLine("STOPCODE", "Ceased to draw the variant thing", Part2LinkBasis.PRACTICE, null),
            ),
            assertNotNull(index.code("FIRSTCODE")).related,
        )
        assertEquals(emptyList(), assertNotNull(index.code("SECONDCODE")).related, "no heading and no row where there is no data")
    }

    @Test
    fun aVariantSerialsPageGathersTheCompanionsOfAnyOfItsCodesOnce() {
        val link = Part2RelatedLink("FIRSTCODE", "practice")
        val crafted = lookup.copy(related = listOf(Part2RelatedRow("VARIANT1", listOf(link)), Part2RelatedRow("VARIANT2", listOf(link))))
        val lines = assertNotNull(Part2LookupIndex(crafted).code("VARIANT1")).related
        assertEquals(listOf("FIRSTCODE"), lines.map { it.code })
    }

    @Test
    fun aCompanionCodeTheLookupDoesNotKnowIsLeftOutRatherThanShownWithoutAName() {
        val crafted = lookup.copy(related = listOf(Part2RelatedRow("FIRSTCODE", listOf(Part2RelatedLink("NOSUCHCODE", "practice")))))
        assertEquals(emptyList(), assertNotNull(Part2LookupIndex(crafted).code("FIRSTCODE")).related)
    }

    @Test
    fun anyCodeOfAVariantSerialOpensTheSameWholePageAndAnUnknownOrLowercaseCodeOpensNone() {
        assertEquals(index.code("VARIANT1"), index.code("VARIANT2"))
        assertNotNull(index.code("STOPCODE"), "search reaches an unbuilt ribbon's codes")
        assertNull(index.code("NOSUCHCODE"))
        assertNull(index.code("firstcode"), "codes are the manual's capitals; a saved or typed lowercase id is not a page")
        assertNull(index.code(""))
        assertNull(assertNotNull(index.code("STOPCODE")).group, "the screen says the manual prints none; the model holds nothing")
        assertTrue(lookup.knowsCode("VARIANT2"))
        assertFalse(lookup.knowsCode("NOSUCHCODE"))
    }

    @Test
    fun searchReachesTheCodesAndNamesOfARibbonWhoseOrdersCannotBeDraftedToo() {
        assertEquals(listOf("3.28"), serials("ceased"))
        assertEquals(listOf("3.27", "3.28"), serials("variant"))
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
        assertEquals(listOf("1.1", "3.1", "3.27"), serials("granted thing"))
        assertEquals(listOf("3.1"), serials("granted fourth"))
        assertEquals(emptyList(), serials("granted second"), "every word must match somewhere")
        assertEquals(listOf("2.1"), serials("released"), "a word start matches")
    }

    @Test
    fun searchByTheSerialNumberFindsExactlyThatSerial() {
        assertEquals(listOf("3.28"), serials("3.28"))
        assertEquals(listOf("3.27"), serials(" 3.27 "))
        assertEquals(emptyList(), serials("3.2"), "a serial is matched whole, so 3.2 is not 3.27 or 3.28")
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
        val empty = Part2LookupIndex(lookup.copy(serials = emptyList(), ribbons = emptyList()))
        assertEquals(emptyList(), empty.ribbons())
        assertEquals(Part2SearchResult.Results(emptyList()), empty.search("anything"))
        assertNull(empty.code("FIRSTCODE"))
    }
}
