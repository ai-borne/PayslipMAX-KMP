package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuideTab
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking

/**
 * Set-up for the draft screen tests: the invented templates (a Group A leave code with a ration order and a hill-station stop, an
 * extension that needs its grant order, a claim) behind the real Guide tab, with a recording drafting service and an in-memory
 * repository. [path] walks Guide Home to the first case, where the "Draft this order" rows are.
 */
abstract class Part2DraftUiBase : Part2UiBase() {
    override fun bundle(): GuideBundle = SyntheticPart2Templates.bundle()

    /** True while the app lock is up: the Guide tab leaves composition, as `App` does, and comes back on unlock. */
    protected var locked by mutableStateOf(false)

    /** [show], but the tab can leave and re-enter composition through [locked], which is what the real app lock does to it. */
    protected fun showLockable(
        nav: GuideNavState = GuideNavState(),
        unlocked: Boolean = true,
    ): GuideNavState {
        composeRule.setContent {
            if (!locked) GuideTab(nav, GuideAccess(unlocked, unlocked) { unlockTaps++ }, guide, search, platform, null, null, factory)
        }
        settle()
        return nav
    }

    protected val day = { month: Int, d: Int -> CalendarDate(2026, month, d) }

    /** The open draft's model, which the host keeps in the nav state while a draft level is on the stack. */
    protected fun model(nav: GuideNavState): LeaveDraftViewModel = checkNotNull(nav.part2Draft) { "no draft is open" }

    protected fun path() {
        tap(Part2Strings.entryTitle)
        tap("The first ribbon")
        tap("First case")
    }

    /** The "Draft this order" button of the [row]th code row (0 is the first, ALPHAGRANT). */
    protected fun draftRow(row: Int = 0) {
        composeRule.onAllNodesWithText(Part2Strings.draftThisOrder)[row].performClick()
        settle()
    }

    protected fun fillLeave(model: LeaveDraftViewModel) {
        model.setDate(0, "leave_from", day(7, 5))
        model.setDate(0, "leave_to", day(9, 4))
        model.setNumber(0, "year", "2026")
        settle()
    }

    protected fun typeInto(
        label: String,
        text: String,
    ) {
        composeRule.onNodeWithText(label).performTextInput(text)
        settle()
    }

    /** A complete saved draft of the first code, as a restored or lapsed officer would hold it. */
    protected fun savedDraft(
        id: String,
        updatedAt: Long = 1_700_000_000_000L,
    ): Part2Draft {
        val values = mapOf("leave_from" to SlotValue.DateValue(day(7, 5)), "leave_to" to SlotValue.DateValue(day(9, 4)), "year" to SlotValue.NumberValue(2026))
        return Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest(SyntheticPart2Templates.GRANT, values))), emptyMap(), 1L, updatedAt).also { runBlocking { repository.saveDraft(it) } }
    }

    protected fun has(text: String) = composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    protected fun assertSlipReady() = composeRule.onNodeWithText(Part2Strings.seeSlip).assertIsEnabled()
}
