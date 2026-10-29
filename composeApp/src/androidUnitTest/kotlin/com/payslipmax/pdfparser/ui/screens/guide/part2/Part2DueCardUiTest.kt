package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueCardModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueImportCard
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the card is the one place Part II speaks to an officer who is not in the Guide, right after an import, so it must show only then,
 * only to someone entitled, only while something is due, say how many, open the Due list on one tap, and go away on "Not now" until the next
 * import. The Dashboard wiring around it is one call; this proves the card. Synthetic drafts only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2DueCardUiTest {
    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val repository = FakePart2Repository()
    private val india = 19_800_000L
    private val now = PreClaimDates.toUtcMillis(CalendarDate(2026, 10, 12)) + 9 * 3_600_000L - india
    private var opened = 0
    private val model: DueCardModel by lazy {
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPart2Templates.bundle())), FakeCrashReporter(), dispatcher)
        val drafts = Part2DraftsViewModel(guide, repository, dispatcher) { india }
        DueCardModel(DueViewModel(repository, drafts, ReminderPlanner { india }, { now }, dispatcher, emptyFlow()))
    }

    private fun dueSlip(id: String = "d1") =
        runBlocking {
            val request = OccurrenceRequest("GTDAL", mapOf("leave_from" to SlotValue.DateValue(CalendarDate(2026, 10, 5)), "leave_to" to SlotValue.DateValue(CalendarDate(2026, 10, 9))))
            repository.saveDraft(Part2Draft(id, LeaveEventInput(listOf(request)), emptyMap(), 1, 1, PrintedSnapshot(now - 40L * 86_400_000L, emptyMap(), emptyList())))
            repository.saveOrder(SyntheticPart2Drafts.order("order-$id", id, "A/12", CalendarDate(2026, 6, 20)))
        }

    private fun settle() {
        repeat(3) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
        }
    }

    private fun show(
        entitled: Boolean = true,
        importDone: () -> Boolean = { true },
    ) {
        composeRule.setContent { DueImportCard(model, entitled, importDone(), onOpen = { opened++ }) }
        settle()
    }

    private fun cardShown() = composeRule.onAllNodesWithText(Part2DueStrings.cardTitle).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun afterAnImportItSaysHowManyAreDueAndOffersTheListAndNotNow() {
        dueSlip("d1")
        dueSlip("d2")
        show()
        composeRule.onNodeWithText(Part2DueStrings.cardBody(2)).assertExists()
        composeRule.onNodeWithText(Part2DueStrings.cardOpen).assertExists()
        composeRule.onNodeWithText(Part2DueStrings.cardNotNow).assertExists()
    }

    @Test
    fun beforeAnyImportThereIsNoCardEvenWithSomethingDue() {
        dueSlip()
        show(importDone = { false })
        assertEquals(false, cardShown())
    }

    @Test
    fun anOfficerWithoutTheEntitlementNeverSeesIt() {
        dueSlip()
        show(entitled = false)
        assertEquals(false, cardShown())
    }

    @Test
    fun withNothingDueThereIsNoCard() {
        show()
        assertEquals(false, cardShown())
    }

    @Test
    fun seeingWhatIsDueCallsTheOpenerOnce() {
        dueSlip()
        show()
        composeRule.onNodeWithText(Part2DueStrings.cardOpen).performClick()
        settle()
        assertEquals(1, opened)
    }

    @Test
    fun notNowHidesTheCardWithoutOpeningAnything() {
        dueSlip()
        show()
        composeRule.onNodeWithText(Part2DueStrings.cardNotNow).performClick()
        settle()
        assertEquals(false, cardShown())
        assertEquals(0, opened)
    }

    @Test
    fun aNewImportBringsTheCardBackAfterNotNow() {
        dueSlip()
        var done by mutableStateOf(true)
        show(importDone = { done })
        composeRule.onNodeWithText(Part2DueStrings.cardNotNow).performClick()
        settle()
        done = false
        settle()
        assertEquals(false, cardShown())
        done = true
        settle()
        assertEquals(true, cardShown())
    }
}
