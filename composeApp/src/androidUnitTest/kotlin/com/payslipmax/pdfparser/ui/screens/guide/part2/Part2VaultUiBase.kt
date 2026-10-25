package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.runBlocking

/**
 * Set-up for the vault screen tests: the real Guide tab behind a fake scanner that answers [scanOf], an in-memory repository and a
 * picture decoder that records what it was handed. [homeWith] walks to the Part II home, where the "Your published orders" section is.
 */
abstract class Part2VaultUiBase : Part2DraftUiBase() {
    /** What the next scan returns: [count] readable pages. */
    protected fun scanOf(count: Int) {
        scanner.next = PageScan.Pages((1..count).map { FakeScannedPage(SyntheticPart2Drafts.jpeg(300, it), name = "page$it") })
    }

    /** An order the device already holds, with [pages] stored pages. */
    protected fun heldOrder(
        id: String,
        no: String? = "A/12",
        date: CalendarDate? = CalendarDate(2026, 10, 12),
        draftId: String? = null,
        pages: Int = 1,
        updatedAt: Long = 1_700_000_000_000L,
    ) {
        runBlocking {
            repository.saveOrder(SyntheticPart2Drafts.order(id, draftId, no, date, updatedAt))
            repeat(pages) { repository.addPage(id, SyntheticPart2Drafts.jpeg(300, it + 1)) }
        }
    }

    protected fun homeRow(
        no: String = "A/12",
        date: CalendarDate = CalendarDate(2026, 10, 12),
    ) = Part2VaultStrings.orderTitle(no, "%02d/%02d/%d".format(date.day, date.month, date.year))

    protected fun openPartTwoHome() {
        tap(Part2Strings.entryTitle)
    }

    protected fun typeNumber(text: String) {
        composeRule.onNodeWithText(Part2Strings.orderNoLabel).performTextInput(text)
        settle()
    }

    protected fun hasNumberBox() = composeRule.onAllNodesWithText(Part2Strings.orderNoLabel).fetchSemanticsNodes().isNotEmpty()

    protected fun anyTextBox() = composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()

    protected fun press(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    /** Taps the first button whose text contains [part] (a row whose second half names a date that depends on the device's time zone). */
    protected fun tapContaining(part: String) {
        composeRule.onAllNodesWithText(part, substring = true).onFirst().performClick()
        settle()
    }

    protected fun orderLevel(
        id: String = "o1",
        unlocked: Boolean = true,
    ) = show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order(id))), part2Unlocked = unlocked)
}
