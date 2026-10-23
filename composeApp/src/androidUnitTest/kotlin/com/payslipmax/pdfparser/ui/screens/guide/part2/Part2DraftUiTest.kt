package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the officer's path is a case, a form that saves as they type, and a slip they can check. These walk it on the real Guide tab with
 * invented templates: the form asks what the orders print, the slip appears when the answers are complete, an ack check must be ticked,
 * and a restored or locked form comes back from the repository with every answer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2DraftUiTest : Part2DraftUiBase() {
    @Test
    fun anEntitledOfficerDraftsFromACaseRowAndTheFormAsksForTheTemplatesFields() {
        val nav = show()
        path()
        draftRow()
        assertEquals(Part2Destination.Draft("draft-1"), nav.current)
        see(Part2Strings.sectionTitle(1, GRANT)).assertIsDisplayed()
        for (label in listOf("First day", "Last day")) see("$label: ${PreClaimStrings.dateChoose}").assertIsDisplayed()
        composeRule.onNodeWithText("Year").assertIsDisplayed()
        see(Part2Strings.question("ration")).assertIsDisplayed()
        see(Part2Strings.stillNeeded(listOf("First day", "Last day", "Year"))).assertIsDisplayed()
        composeRule.onNodeWithText(Part2Strings.seeSlip).assertIsNotEnabled()
        assertNull(runBlocking { repository.draft("draft-1") }, "nothing is saved until the officer changes something")
    }

    @Test
    fun typingSavesTheDraftAndTheSlipOpensOnceTheAnswersAreComplete() {
        val nav = show()
        path()
        draftRow()
        typeInto("Year", "2026")
        assertNotNull(runBlocking { repository.draft("draft-1") }, "saved after the pause")
        fillLeave(model(nav))
        assertSlipReady()
        tap(Part2Strings.seeSlip)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        see("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026").assertIsDisplayed()
        see(Part2Strings.orderTitle(1, listOf("A"))).assertIsDisplayed()
        see(Part2Strings.lastOrderBlank).assertIsDisplayed()
        tap(Part2Strings.editDraft)
        assertEquals(Part2Destination.Draft("draft-1"), nav.current, "Edit returns to the form below instead of stacking a second one")
        assertEquals(5, nav.stack.size, "Guide Home, Part II home, ribbon, case, form: no second form was stacked")
    }

    @Test
    fun anAckCheckIsShownWithItsCitationAndMustBeTickedBeforeTheSlipIsReady() {
        val nav = show()
        path()
        draftRow(1)
        val draft = model(nav)
        draft.setDate(0, "ext_from", day(9, 5))
        draft.setDate(0, "new_to", day(9, 20))
        settle()
        tap(Part2Strings.seeSlip)
        see(Part2Strings.ackHeading).assertIsDisplayed()
        see(Part2Strings.findingUnknown).assertIsDisplayed()
        see(Part2Strings.citation(com.payslipmax.pdfparser.part2.domain.LinkBasis.MANUAL, 131, null)).assertIsDisplayed()
        composeRule.onAllNodesWithText(Part2Strings.readyToPrint).assertCountEquals(0)
        tap(Part2Strings.ackTick)
        see(Part2Strings.readyToPrint).assertIsDisplayed()
        assertTrue(draft.state.value.slip!!.readyToPrint)
    }

    @Test
    fun aRestoredStackOpensTheSavedFormWithEveryAnswerAndKeepsSaving() {
        savedDraft("draft-7")
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("draft-7"))))
        assertEquals(Part2Destination.Draft("draft-7"), nav.current)
        composeRule.onNodeWithText("2026").assertIsDisplayed()
        assertSlipReady()
        typeInto("Year", "7")
        assertEquals(1, models.size)
    }

    @Test
    fun theLockDropsTheFormAndTheOfficerComesBackToTheSameDraftFromTheDatabase() {
        val nav = showLockable()
        path()
        draftRow()
        fillLeave(model(nav))
        val before = model(nav)
        locked = true
        nav.discardTransientForms()
        settle()
        locked = false
        settle()
        assertEquals(Part2Destination.Draft("draft-1"), nav.current, "the lock keeps the stack")
        val after = model(nav)
        assertTrue(after !== before, "a new model was read back from the repository")
        assertSlipReady()
    }

    @Test
    fun aDraftTheRepositoryNoLongerHoldsIsCutBackToPartTwoHome() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("gone"))))
        assertEquals(Part2Destination.Home, nav.current)
        assertNull(nav.part2Draft)
    }

    @Test
    fun anotherOccurrenceCanBeAddedAndRemovedOnTheForm() {
        val nav = show()
        path()
        draftRow()
        tap(EXTENSION)
        see(Part2Strings.sectionTitle(2, EXTENSION)).assertIsDisplayed()
        assertEquals(2, model(nav).state.value.sections.size)
        tap(Part2Strings.removeOccurrence)
        assertEquals(1, model(nav).state.value.sections.size)
    }

    @Test
    fun theUpgradePanelClosesWhenPremiumReturns() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Locked)))
        assertEquals(Part2Destination.Home, nav.current, "an entitled user is never left on the upgrade panel")
    }

    @Test
    fun aHeldModelOfAnotherDraftIsNeverShownForThisOne() {
        savedDraft("draft-a")
        runBlocking { repository.saveDraft(savedDraft("draft-b").copy(event = LeaveEventInput(listOf(OccurrenceRequest(EXTENSION))))) }
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("draft-b")))
        guide.load()
        dispatcher.scheduler.advanceUntilIdle()
        nav.part2Draft = assertNotNull(drafting.open("draft-a"), "the Guide has loaded, so a model of the other draft exists to be wrongly reused")
        show(nav)
        see(Part2Strings.sectionTitle(1, EXTENSION)).assertIsDisplayed()
        assertEquals("draft-b", model(nav).id, "the level builds the model of its own draft")
    }
}
