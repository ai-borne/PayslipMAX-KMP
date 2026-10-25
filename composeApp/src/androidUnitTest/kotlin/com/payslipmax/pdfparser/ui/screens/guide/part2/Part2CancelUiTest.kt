package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.CancelDrafts
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2CancelStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: para 11 (p.8) says a wrong order is cancelled and notified afresh, so from any vault order the officer must be able to start that in
 * one tap: a CANCEL slip quoting the order, and, when the order came from a draft the device still holds, a corrected draft copied from
 * it. Run on the shipped templates and rules. The offer follows the entitlement and the order (it must be quotable), and the pair of
 * slips, the print record and the vault's note all hang together. Synthetic orders only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2CancelUiTest : Part2VaultUiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    override fun bundle(): GuideBundle = real

    override fun rules(): Part2Rules = Part2Rules.shipped()

    private val quote = "Cancelled all occurrence of Part II Order Number A/12 dated 12/10/2026"

    /** A complete annual leave the order "came from". */
    private fun leaveDraft(id: String = "src"): Part2Draft {
        val values = mapOf("leave_from" to SlotValue.DateValue(day(7, 6)), "leave_to" to SlotValue.DateValue(day(8, 4)), "year" to SlotValue.NumberValue(2026))
        return Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest("GTDAL", values))), emptyMap(), 1L, 5L).also { runBlocking { repository.saveDraft(it) } }
    }

    private fun draftOf(id: String) = runBlocking { repository.draft(id) }

    @Test
    fun aQuotableOrderOffersToCancelItAndOffersTheRedraftOnlyWhileItsDraftExists() {
        heldOrder("o1", draftId = null)
        orderLevel()
        see(Part2CancelStrings.cancelOrder).assertIsDisplayed()
        assertFalse(has(Part2CancelStrings.cancelAndRedraft), "no draft to copy, so no re-draft")
    }

    @Test
    fun anOrderWithItsDraftOffersBothAndAMissingDraftOffersOnlyTheCancel() {
        leaveDraft()
        heldOrder("o1", draftId = "src")
        orderLevel()
        see(Part2CancelStrings.cancelOrder).assertIsDisplayed()
        see(Part2CancelStrings.cancelAndRedraft).assertIsDisplayed()
    }

    @Test
    fun anOrderThatCameFromACancelSlipOffersTheCancelButNotTheRedraft() {
        runBlocking { repository.saveDraft(Part2Draft("slip", CancelDrafts.cancelEvent(OrderRef("A/1", CalendarDate(2026, 6, 20))), emptyMap(), 1L, 5L)) }
        heldOrder("o1", draftId = "slip")
        orderLevel()

        see(Part2CancelStrings.cancelOrder).assertIsDisplayed()
        assertFalse(has(Part2CancelStrings.cancelAndRedraft))
    }

    @Test
    fun anOrderWithoutItsNumberAndDateSaysWhyItCannotBeCancelledYet() {
        heldOrder("o1", no = null, date = null)
        orderLevel()

        see(Part2CancelStrings.cancelNeedsNumber).assertIsDisplayed()
        assertFalse(has(Part2CancelStrings.cancelOrder))
        assertFalse(has(Part2CancelStrings.cancelAndRedraft))
    }

    @Test
    fun anOfficerWhosePremiumEndedNeitherSeesTheOfferNorTheHint() {
        leaveDraft()
        heldOrder("o1", draftId = "src")
        orderLevel(unlocked = false)

        assertFalse(has(Part2CancelStrings.cancelOrder))
        assertFalse(has(Part2CancelStrings.cancelAndRedraft))
        assertFalse(has(Part2CancelStrings.cancelNeedsNumber))
    }

    @Test
    fun cancellingOpensTheCancelSlipQuotingTheOrderWithNoPartnerAndTheOrderIsUntouched() {
        heldOrder("o1", draftId = null)
        val nav = orderLevel()

        tap(Part2CancelStrings.cancelOrder)

        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        see(quote).assertIsDisplayed()
        assertFalse(has(Part2CancelStrings.openCorrectedDraft))
        assertNull(draftOf("draft-1")?.pairedWith)
        assertEquals("A/12", runBlocking { repository.order("o1") }?.orderNo)
        assertEquals(setOf("draft-1"), runBlocking { repository.observeDrafts().first() }.items.map { it.id }.toSet(), "one slip and nothing else")
    }

    @Test
    fun cancelAndRedraftOpensTheCancelSlipThenHopsToTheCorrectedDraftAndBackWithoutGrowingTheStack() {
        leaveDraft()
        heldOrder("o1", draftId = "src")
        val nav = orderLevel()

        tap(Part2CancelStrings.cancelAndRedraft)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        see(quote).assertIsDisplayed()
        val depth = nav.stack.size

        tap(Part2CancelStrings.openCorrectedDraft)
        assertEquals(Part2Destination.Slip("draft-2"), nav.current)
        assertTrue(has("GTDAL"), "the corrected draft is the leave draft copied")
        assertEquals(depth + 1, nav.stack.size)

        tap(Part2CancelStrings.openCancelSlip)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        assertEquals(depth, nav.stack.size, "hopping back pops, so Back never walks a loop")
        assertEquals("draft-2", draftOf("draft-1")?.pairedWith)
        assertEquals("draft-1", draftOf("draft-2")?.pairedWith)
    }

    @Test
    fun printingTheCancelSlipIsRecordedAndTheVaultRowAndTheOrderNowSayTheSlipWasPrinted() {
        leaveDraft()
        heldOrder("o1", draftId = "src")
        val nav = orderLevel()
        tap(Part2CancelStrings.cancelAndRedraft)

        tap(Part2Strings.printButton)

        assertNotNull(draftOf("draft-1")?.printed, "the print record is saved on the cancel slip")
        assertNull(draftOf("draft-2")?.printed, "the corrected draft is a separate slip with its own record")
        back()
        assertEquals(Part2Destination.Order("o1"), nav.current)
        assertTrue(has("Cancel slip printed on"))
        back()
        assertTrue(has("Cancel slip printed on"), "the same note is on the vault row")
    }

    @Test
    fun theCancelFormFromTheRecordsCaseOffersTheVaultForTheOrderBeingCancelledAndAPickFillsTheQuote() {
        heldOrder("o1", no = "A/12", date = CalendarDate(2026, 10, 12), draftId = null)
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap("Records and unit-only orders")
        tap("Cancelling a published order")
        draftRow(0)

        tap(Part2VaultStrings.useVaultOrder)
        tap(Part2VaultStrings.grantChoice("A/12", "12/10/2026"))
        tap(Part2Strings.seeSlip)

        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        see(quote).assertIsDisplayed()
    }

    @Test
    fun aCancelSlipOffersOnlyTheCancelCodesToAddAndALeaveSlipOffersNone() {
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap("Records and unit-only orders")
        tap("Cancelling a published order")
        draftRow(0)
        assertEquals(listOf("CANCEL", "CANCELS"), model(nav).state.value.addable.map { it.code })

        val leave = leaveDraft("lv")
        val leaveModel = drafting.open(leave.id)!!
        settle()
        assertTrue(leaveModel.state.value.addable.none { it.code in setOf("CANCEL", "CANCELS") })
        assertTrue(leaveModel.state.value.addable.any { it.code == "EXTNAL" })
    }

    @Test
    fun aCancelThatCannotStartSaysWhyOnTheOrderScreenOpensNothingAndTheNoticeGoesOnceOneStarts() {
        leaveDraft()
        heldOrder("o1", draftId = "src")
        val nav = orderLevel()
        runBlocking { repository.deleteDraft("src") }

        tap(Part2CancelStrings.cancelAndRedraft)

        see(Part2CancelStrings.nothingToCopy).assertIsDisplayed()
        assertEquals(Part2Destination.Order("o1"), nav.current)
        assertEquals(emptySet(), runBlocking { repository.observeDrafts().first() }.items.map { it.id }.toSet(), "no half pair")

        tap(Part2CancelStrings.cancelOrder)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        back()
        assertFalse(has(Part2CancelStrings.nothingToCopy), "starting a cancel clears the earlier refusal")
    }
}
