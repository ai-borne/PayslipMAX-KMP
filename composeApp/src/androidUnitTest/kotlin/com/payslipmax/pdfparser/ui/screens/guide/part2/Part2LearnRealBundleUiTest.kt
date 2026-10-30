package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY: the invented section proves the screens; this proves the shipped one. On the real bundle the Learn home lists all ten ribbons, a
 * casualty page shows the manual's own facts, the 19 casualties whose required columns print no caption point to their page, and
 * "Usually published with" appears for exactly the casualties that have companions. No manual wording is hard-coded: every expectation is
 * read from the bundle, so a content update does not break it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2LearnRealBundleUiTest : Part2UiBase() {
    private val shipped: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }
    private val lookup = assertNotNull(shipped.part2)

    override fun bundle(): GuideBundle = shipped

    /** One composition per test: the first call shows the page, later calls swap the casualty page on the same stack. */
    private var nav: GuideNavState? = null

    private fun open(code: String) {
        val shown = nav
        if (shown == null) {
            nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code(code))))
        } else {
            shown.pop()
            shown.push(Part2Destination.Code(code))
            settle()
        }
    }

    @Test
    fun theHomeListsAllTenRibbonsOfTheShippedManual() {
        show()
        tap(LearnStrings.entryTitle)
        assertTrue(lookup.ribbons.size == 10)
        lookup.ribbons.forEach { see(it.title).assertIsDisplayed() }
    }

    @Test
    fun aCasualtyPageShowsTheManualsFactsFromTheBundle() {
        val page = lookup.serials.first { it.serial == "5.1" }
        open(page.codes.first())
        see(page.name).assertIsDisplayed()
        see(LearnStrings.chapterAndPage(page.chapter, page.page)).assertIsDisplayed()
        see(LearnStrings.groupLabel(page.group!!)).assertIsDisplayed()
        see(lookup.groups.getValue(page.group!!)).assertIsDisplayed()
    }

    @Test
    fun everyRequiredColumnThatPrintsNoCaptionPointsToItsPageAndNoOtherDoes() {
        val bare = lookup.columns.filter { row -> (listOf(row.from, row.to) + row.data).any { it.need == "required" && it.caption.isEmpty() } }
        assertTrue(bare.size == 19, "the bundle holds the 19 casualties the owner counted")
        for (row in bare) {
            val page = lookup.serials.first { it.serial == row.serial }
            val missing = (listOf(row.from, row.to) + row.data).count { it.need == "required" && it.caption.isEmpty() }
            open(page.codes.first())
            count(LearnStrings.requiredNoCaption(page.page)).assertCountEquals(missing)
        }
    }

    @Test
    fun theWrittenPagesAndTheCompanionsAppearExactlyWhereTheBundleHasThem() {
        val withAbout = lookup.about.flatMap { it.serials }.toSet()
        val withRelated = lookup.related.map { it.code }.toSet()
        for (page in lookup.serials.filter { it.serial in setOf("5.1", "3.1", "5.28", "13.1", "14.1", "2.2", "3.58") }) {
            open(page.codes.first())
            count(LearnStrings.aboutHeading).assertCountEquals(if (page.serial in withAbout) 1 else 0)
            count(LearnStrings.relatedHeading).assertCountEquals(if (page.codes.any { it in withRelated }) 1 else 0)
        }
    }
}
