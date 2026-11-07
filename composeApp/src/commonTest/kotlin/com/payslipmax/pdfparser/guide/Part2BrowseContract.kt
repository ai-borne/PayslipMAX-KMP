package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.Part2LookupIndex
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped Part II lookup and the Learn screens (Orders Bank plan, Phase 2), on the real bundle: a tile and a flat
 * list for each of the ten ribbons with every casualty listed once, every one of the 303 code pages reachable by its serial, by each of
 * its 321 codes and by its name, and the two blank groups left blank. Run on each platform (`GuideBundleAndroidContractTest`,
 * `GuideLoaderIosPerfTest`, which also times the walk on Native).
 */
object Part2BrowseContract {
    /** The longest query the screen lets the officer keep (`MAX_SEARCH_QUERY_LENGTH`). */
    private const val QUERY_CUT = 100

    private fun index(bundle: GuideBundle) = Part2LookupIndex(assertNotNull(bundle.part2, "the bundle ships the Part II lookup"))

    private fun Part2SearchResult.rows() = assertIs<Part2SearchResult.Results>(this).rows

    fun assertEveryRibbonIsOneFlatListOfItsCasualtiesUnderItsCaseNames(bundle: GuideBundle) {
        val index = index(bundle)
        val section = assertNotNull(bundle.part2)
        val tiles = index.ribbons()
        assertEquals(listOf("leave", "moves", "field", "career", "exit", "family", "health", "role", "depu", "records"), tiles.map { it.id }, "all ten, built or not")
        assertEquals(listOf(41, 23, 23, 31, 20, 33, 18, 34, 28, 52), tiles.map { it.serialCount })
        val listed = mutableListOf<String>()
        for (ribbon in section.ribbons) {
            val content = assertNotNull(index.ribbon(ribbon.id), ribbon.id)
            assertEquals(ribbon.cases.map { it.title }, content.sections.map { it.title }, "the case names are the headings of ${ribbon.id}")
            for ((case, shown) in ribbon.cases.zip(content.sections)) assertEquals(case.serials, shown.rows.map { it.serial }, "${case.id} lists the casualties it homes")
            listed += content.sections.flatMap { s -> s.rows.map { it.serial } }
        }
        assertEquals(303, listed.size)
        assertEquals(303, listed.toSet().size, "each casualty is listed once, so a flat list never repeats one")
        assertNull(index.ribbon("nope"))
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
}
