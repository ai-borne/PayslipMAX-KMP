package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.LinkBasis
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The officer's walk on the templates and rules the app ships: annual leave with ration and R&H, answered through the form's own
 * controls, becomes one A-group order of seven rows in date order, with the practice rows labelled as practice; a leave that breaks the
 * 60-day note shows the manual's citation and must be ticked. Dates, station and notification are invented.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2DraftRealBundleUiTest : Part2DraftUiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    override fun bundle(): GuideBundle = real

    override fun rules(): Part2Rules = Part2Rules.shipped()

    private fun openAnnualLeaveForm() {
        tap(Part2Strings.entryTitle)
        tap("Leave")
        tap("Annual and casual leave")
        draftRow()
    }

    @Test
    fun annualLeaveWithRationAndRhAnsweredOnTheFormGivesOneOrderOfSevenRowsInDateOrder() {
        val nav = show()
        openAnnualLeaveForm()
        assertEquals(Part2Destination.Draft("draft-1"), nav.current)
        tap(Part2Strings.question("ration"))
        tap(AreaRules.rhCodes[1])
        val draft = model(nav)
        draft.setDate(0, "prefix_first", day(7, 4))
        draft.setDate(0, "leave_from", day(7, 6))
        draft.setDate(0, "leave_to", day(8, 4))
        draft.setNumber(0, "year", "2026")
        draft.setText(0, "leave_kind", "annual leave")
        draft.setText(0, "station", "ZZ01")
        draft.setText(0, "corps", "Alpha")
        draft.setReference(0, "notif", "N-17/2026", day(3, 2))
        draft.setText(0, "serial", "12")
        settle()
        composeRule.onNodeWithText(Part2Strings.seeSlip).assertIsEnabled()
        tap(Part2Strings.seeSlip)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        see(Part2Strings.orderTitle(1, listOf("A"))).assertIsDisplayed()
        val rows = draft.state.value.slip!!.orders.single().rows
        assertEquals(listOf("SORS", "GTDRSHNA", "STPRH", "GTDAL", "TORS", "STPRSHNA", "GTDRH12"), rows.map { it.code })
        val link = real.part2!!.templates.first { it.code == "GTDAL" }.links.first { it.code == "SORS" }
        assertEquals(Part2Strings.citation(LinkBasis.MANUAL, link.page, link.note), rows.first { it.code == "SORS" }.source, "the source is the link's own citation, read from the template")
        assertEquals(Part2Strings.seenInPractice, rows.first { it.code == "STPRH" }.source)
        assertEquals("", rows.first { it.code == "GTDAL" }.source)
    }

    @Test
    fun aLeaveOverTheSixtyDayNoteShowsTheManualsCitationAndMustBeTicked() {
        val nav = show()
        openAnnualLeaveForm()
        val draft = model(nav)
        draft.setDate(0, "leave_from", day(7, 6))
        draft.setDate(0, "leave_to", day(8, 4))
        draft.setNumber(0, "year", "2026")
        draft.setNumber(0, "earlier_al", "40")
        settle()
        tap(Part2Strings.seeSlip)
        see(Part2Strings.finding("al_year_cap")!!).assertIsDisplayed()
        see(Part2Strings.citation(LinkBasis.MANUAL, 115, "2")).assertIsDisplayed()
        assertTrue(!draft.state.value.slip!!.readyToPrint)
        tap(Part2Strings.ackTick)
        assertTrue(draft.state.value.slip!!.readyToPrint)
    }
}
