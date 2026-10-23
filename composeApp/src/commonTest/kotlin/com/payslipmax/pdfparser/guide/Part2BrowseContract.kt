package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.Part2LookupIndex
import com.payslipmax.pdfparser.part2.domain.Part2PrintedAs
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped Part II lookup and the browse screens (Part II plan, phase 4a), on the real bundle: tiles only for the
 * built ribbons, every one of the 303 code pages reachable by its serial, by each of its 321 codes and by its name (Round 8), the two
 * blank groups left blank, and REJOINAL the only code whose slip word differs. Run on each platform (`GuideBundleAndroidContractTest`,
 * `GuideLoaderIosPerfTest`, which also times the walk on Native).
 */
object Part2BrowseContract {
    /** The longest query the screen lets the officer keep (`MAX_SEARCH_QUERY_LENGTH`). */
    private const val QUERY_CUT = 100

    private fun index(bundle: GuideBundle) = Part2LookupIndex(assertNotNull(bundle.part2, "the bundle ships the Part II lookup"))

    private fun Part2SearchResult.rows() = assertIs<Part2SearchResult.Results>(this).rows

    fun assertTilesShowOnlyForTheBuiltRibbons(bundle: GuideBundle) {
        val index = index(bundle)
        val tiles = index.ribbons()
        assertEquals(listOf("leave", "records"), tiles.map { it.id }, "Leave and the lookup-only Records ribbon, and no other")
        assertEquals(listOf(6, 7), tiles.map { it.caseCount })
        for (hidden in listOf("moves", "field", "career", "exit", "family", "health", "role", "depu")) assertNull(index.ribbon(hidden), hidden)
        val leave = assertNotNull(index.ribbon("leave"))
        assertEquals(30, leave.cases.sumOf { it.codeCount }, "Leave homes all of Chapter 5")
        assertTrue(leave.cases.all { it.span.chapters == listOf(5) && it.span.firstPage <= it.span.lastPage }, "Leave is Chapter 5, and its pages read in order")
        for (case in leave.cases) {
            val page = assertNotNull(index.case(case.id))
            assertEquals(case.codeCount, page.rows.size)
        }
    }

    fun assertEveryCodePageIsReachableBySerialCodeAndName(bundle: GuideBundle) {
        val index = index(bundle)
        val pages = assertNotNull(bundle.part2).serials
        assertEquals(303, pages.size)
        for (page in pages) {
            assertTrue(index.search(page.serial).rows().any { it.serial == page.serial }, "serial ${page.serial}")
            for (code in page.codes) {
                assertEquals(page.serial, assertNotNull(index.code(code), code).serial, "the page for $code")
                val first = index.search(code).rows().first()
                assertEquals(page.serial to code, first.serial to first.openCode, "searching $code finds its own page first, opened on that code")
            }
            val byName = index.search(page.name.take(QUERY_CUT)).rows()
            assertTrue(byName.any { it.serial == page.serial }, "the name of ${page.serial}")
        }
    }

    fun assertOnlyTheTwoBlankGroupsAreBlankAndEveryOtherGroupHasItsMeaning(bundle: GuideBundle) {
        val index = index(bundle)
        val pages = assertNotNull(bundle.part2).serials
        val blank = pages.filter { index.code(it.codes.first())!!.group == null }.map { it.serial }
        assertEquals(setOf("3.28", "11.8"), blank.toSet(), "no group is inferred")
        for (page in pages.filter { it.group != null }) {
            val group = assertNotNull(index.code(page.codes.first())!!.group)
            assertEquals(page.group, group.letter)
            assertTrue(group.meaning.isNotBlank(), "${page.serial}: what ${group.letter} means")
        }
    }

    fun assertRejoinalIsTheOnlyCodeThePageSaysTheSlipPrintsDifferently(bundle: GuideBundle) {
        val index = index(bundle)
        val pages = assertNotNull(bundle.part2).serials
        assertEquals(listOf(Part2PrintedAs("REJOINAL", "REJOIN")), index.code("REJOINAL")!!.printedAs)
        val others = pages.flatMap { it.codes }.filter { it != "REJOINAL" }
        assertTrue(others.all { index.code(it)!!.printedAs.isEmpty() })
    }
}
