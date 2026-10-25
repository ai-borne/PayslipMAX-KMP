package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: an order is linked to the draft it came from at the moment it is scanned ("Add the published order" on the slip), and the slip then
 * lists it, so the officer can find the order a slip became. The link is set once; an officer whose Premium ended still sees and opens the
 * linked orders but cannot add one. The slip's "last Part II order" line follows the vault.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2VaultSlipUiTest : Part2VaultUiBase() {
    private fun slipOf(
        draft: String = "draft-1",
        unlocked: Boolean = true,
    ): GuideNavState {
        savedDraft(draft)
        return show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Slip(draft))), part2Unlocked = unlocked)
    }

    @Test
    fun addingThePublishedOrderScansItLinkedToThisDraftAndOpensIt() {
        scanOf(1)
        val nav = slipOf()

        tap(Part2VaultStrings.addPublishedOrder)

        assertEquals(Part2Destination.Order("order-1"), nav.current)
        assertEquals("draft-1", runBlocking { repository.order("order-1") }?.draftId)
        assertEquals(1, runBlocking { repository.pages("order-1") }.size)
    }

    @Test
    fun backFromTheNewOrderReturnsToTheSlipWhichNowListsIt() {
        scanOf(1)
        slipOf()
        tap(Part2VaultStrings.addPublishedOrder)

        back()

        see(Part2VaultStrings.linkedOrdersHeading).assertIsDisplayed()
        see(Part2VaultStrings.numberNotTyped).assertIsDisplayed()
    }

    @Test
    fun aLinkedOrderIsListedWithItsNumberAndOpensFromTheSlip() {
        heldOrder("o1", draftId = "draft-1")
        heldOrder("o2", no = "B/9", draftId = "another-draft")
        val nav = slipOf()

        see(homeRow()).assertIsDisplayed()
        assertFalse(has(homeRow("B/9")), "another draft's order is not this slip's")
        tap(homeRow())
        assertEquals(Part2Destination.Order("o1"), nav.current)
    }

    @Test
    fun theSlipsLastOrderLineFollowsTheVault() {
        heldOrder("o1", no = "A/12", date = CalendarDate(2026, 10, 12))
        slipOf()
        see(Part2Strings.lastOrderLine("A/12", "12/10/2026")).assertIsDisplayed()
    }

    @Test
    fun aLapsedOfficerSeesTheLinkedOrderButCannotAddAnother() {
        heldOrder("o1", draftId = "draft-1")
        slipOf(unlocked = false)

        see(homeRow()).assertIsDisplayed()
        assertFalse(has(Part2VaultStrings.addPublishedOrder))
    }

    @Test
    fun aSlipWithNothingLinkedShowsNoOrdersHeadingButOffersToAddOne() {
        slipOf()
        assertFalse(has(Part2VaultStrings.linkedOrdersHeading))
        see(Part2VaultStrings.addPublishedOrder).assertIsDisplayed()
    }

    @Test
    fun aDraftNotFinishedHasNoOrderToAdd() {
        runBlocking { repository.saveDraft(SyntheticPart2Drafts.emptyDraft("draft-9")) }
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Slip("draft-9"))))
        assertFalse(has(Part2VaultStrings.addPublishedOrder))
        assertTrue(has(Part2Strings.draftNotFinished))
    }

    @Test
    fun aCancelledScanFromTheSlipLeavesTheSlipAndNoOrder() {
        val nav = slipOf()
        tap(Part2VaultStrings.addPublishedOrder)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        assertNull(runBlocking { repository.order("order-1") })
    }

    @Test
    fun theOrderScreenOffersTheDraftItCameFromOnlyWhileTheDraftExists() {
        savedDraft("draft-1")
        heldOrder("o1", draftId = "draft-1")
        heldOrder("o2", draftId = "draft-gone")
        val nav = show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap(Part2VaultStrings.openDraft)
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)

        nav.cutDraft("draft-1")
        nav.push(Part2Destination.Order("o2"))
        settle()
        assertFalse(has(Part2VaultStrings.openDraft))
    }

    @Test
    fun openingTheDraftFromItsOrderReturnsToTheSlipInsteadOfStackingAnother() {
        val nav = slipOf()
        heldOrder("o1", draftId = "draft-1")
        settle()
        tap(homeRow())
        val depth = nav.stack.size

        tap(Part2VaultStrings.openDraft)

        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
        assertEquals(depth - 1, nav.stack.size, "back to the slip below, not a second one on top")
    }

    private fun draftOf(id: String): Part2Draft = checkNotNull(runBlocking { repository.draft(id) })

    @Test
    fun theAddButtonIsDisabledWhileAScanIsOpen() {
        scanOf(1)
        scanner.gate = CompletableDeferred()
        slipOf()

        tap(Part2VaultStrings.addPublishedOrder)
        see(Part2VaultStrings.addPublishedOrder).assertIsNotEnabled()

        scanner.gate?.complete(Unit)
        settle()
    }

    @Test
    fun whatAScanFromTheSlipSaysIsShownOnTheSlipAndCanBeDismissed() {
        scanner.next = PageScan.Failed(ScanFailure.FAILED)
        slipOf()

        tap(Part2VaultStrings.addPublishedOrder)
        see(Part2VaultStrings.scanFailed).assertIsDisplayed()
        press(Part2VaultStrings.noticeDismiss)

        assertFalse(has(Part2VaultStrings.scanFailed))
    }
}
