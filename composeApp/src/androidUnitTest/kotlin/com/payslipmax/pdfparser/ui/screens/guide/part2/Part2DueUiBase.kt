package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.onAllNodesWithText
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import kotlinx.coroutines.runBlocking

/**
 * Set-up for the Due screen tests: the real Guide tab on the shipped bundle and rules, with printed leave slips the officer "made" saved in an
 * in-memory repository. The clock is [dueNow] (12 October 2026, 09:00 in India). A leave that ended on 9 October has its return due on the
 * 10th, so it is overdue; one that ends on 20 October is coming up. All dates and numbers are synthetic.
 */
abstract class Part2DueUiBase : Part2UiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    override fun bundle(): GuideBundle = real

    override fun rules(): Part2Rules = Part2Rules.shipped()

    private fun day(
        month: Int,
        day: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, month, day))

    /** A printed annual-leave slip, with its numbered order linked unless [published] is false (then its add-the-order series runs too). */
    protected fun hold(
        id: String,
        lastDay: Int = 9,
        published: Boolean = true,
    ) {
        val request = OccurrenceRequest("GTDAL", mapOf("leave_from" to day(10, 5), "leave_to" to day(10, lastDay)))
        runBlocking {
            repository.saveDraft(Part2Draft(id, LeaveEventInput(listOf(request)), emptyMap(), 1, 1, PrintedSnapshot(printedAt, emptyMap(), emptyList())))
            if (published) repository.saveOrder(SyntheticPart2Drafts.order("order-$id", id, "A/12", CalendarDate(2026, 6, 20)))
        }
    }

    /** The first of October 2026, so any add-the-order series starts before [dueNow] only if a test wants it. */
    private val printedAt get() = dueNow - 40L * 86_400_000L

    protected fun atDue(): GuideNavState = GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Due))

    /** How many nodes say exactly "Due": the home's row, or the screen's title. */
    protected fun dueTitles() = composeRule.onAllNodesWithText(Part2DueStrings.rowTitle).fetchSemanticsNodes().size
}
