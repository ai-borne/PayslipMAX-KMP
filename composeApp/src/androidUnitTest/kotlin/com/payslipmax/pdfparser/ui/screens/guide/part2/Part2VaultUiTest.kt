package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.CompletableDeferred
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
 * WHY: the vault is where an officer scans a published order in, types its number and date, and finds it again. The scan must open only
 * for someone entitled, a scan that saved nothing must leave nothing, and an officer whose Premium ended must still be able to read and
 * delete what they scanned (Round 13) without being able to change or add to it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2VaultUiTest : Part2VaultUiBase() {
    @Test
    fun anEntitledOfficerWithNoOrdersSeesTheSectionAndAScanButton() {
        show()
        openPartTwoHome()
        see(Part2VaultStrings.vaultHeading).assertIsDisplayed()
        see(Part2VaultStrings.scanOrder).assertIsEnabled()
    }

    @Test
    fun aFreeOfficerWithNothingStoredSeesNoVaultAtAll() {
        show(part2Unlocked = false)
        openPartTwoHome()
        assertFalse(has(Part2VaultStrings.vaultHeading), "nothing to show and nothing they may do")
        assertFalse(has(Part2VaultStrings.scanOrder))
    }

    @Test
    fun anOfficerWhoOnlyHoldsADraftButNoOrderSeesNoVaultEither() {
        savedDraft("draft-1")
        show(part2Unlocked = false)
        openPartTwoHome()
        assertTrue(has(Part2Strings.draftsHeading))
        assertFalse(has(Part2VaultStrings.vaultHeading), "a lapsed officer cannot scan, so an empty vault is not offered")
    }

    @Test
    fun scanningAnOrderInOpensItAndSavesItsPagesEncrypted() {
        scanOf(2)
        val nav = show()
        openPartTwoHome()

        tap(Part2VaultStrings.scanOrder)

        assertEquals(Part2Destination.Order("order-1"), nav.current)
        see(Part2VaultStrings.orderScreenTitle).assertIsDisplayed()
        see(Part2VaultStrings.pagesSaved(2)).assertIsDisplayed()
        assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX), scanner.limits)
        assertEquals(2, runBlocking { repository.pages("order-1") }.size)
        assertNull(runBlocking { repository.order("order-1") }?.draftId, "scanned from the vault, so linked to no draft")
    }

    @Test
    fun aCancelledScanLeavesNothingAndSaysNothing() {
        scanner.next = PageScan.Cancelled
        val nav = show()
        openPartTwoHome()

        tap(Part2VaultStrings.scanOrder)

        assertEquals(Part2Destination.Home, nav.current)
        assertFalse(has(Part2VaultStrings.noticeDismiss))
        assertNull(runBlocking { repository.order("order-1") })
    }

    @Test
    fun aScannerThatNeedsAConnectionExplainsItAndTheMessageCanBeDismissed() {
        scanner.next = PageScan.Failed(ScanFailure.UNAVAILABLE)
        show()
        openPartTwoHome()

        tap(Part2VaultStrings.scanOrder)
        see(Part2VaultStrings.scanNeedsConnection).assertIsDisplayed()
        press(Part2VaultStrings.noticeDismiss)

        assertFalse(has(Part2VaultStrings.scanNeedsConnection))
    }

    @Test
    fun openingThePartTwoHomeClearsWhatAKilledScanLeftBehind() {
        show()
        assertEquals(0, scanner.leftoversDiscarded, "the Guide's front page is not the vault")
        openPartTwoHome()
        assertEquals(1, scanner.leftoversDiscarded)
    }

    @Test
    fun theAppLockClearsTheScannersLeftoversAsTheGuideLeavesTheScreen() {
        showLockable()
        openPartTwoHome()
        val before = scanner.leftoversDiscarded

        locked = true
        settle()

        assertEquals(before + 1, scanner.leftoversDiscarded)
    }

    // ---- a lapsed officer ----

    @Test
    fun anOfficerWhosePremiumEndedStillSeesTheirOrdersButCannotScan() {
        heldOrder("o1")
        show(part2Unlocked = false)
        openPartTwoHome()

        see(homeRow()).assertIsDisplayed()
        assertFalse(has(Part2VaultStrings.scanOrder))
    }

    @Test
    fun deletingAsksFirstAndCancelKeepsTheOrder() {
        heldOrder("o1")
        show()
        openPartTwoHome()

        press(Part2VaultStrings.deleteOrder)
        see(Part2VaultStrings.deleteOrderTitle).assertIsDisplayed()
        press(Part2Strings.deleteCancel)
        assertNotNull(runBlocking { repository.order("o1") })

        press(Part2VaultStrings.deleteOrder)
        press(Part2Strings.deleteConfirm)
        assertNull(runBlocking { repository.order("o1") })
        assertFalse(has(homeRow()))
    }

    // ---- one order ----

    @Test
    fun theScanButtonIsDisabledWhileAScanIsOpen() {
        scanOf(1)
        scanner.gate = CompletableDeferred()
        show()
        openPartTwoHome()

        tap(Part2VaultStrings.scanOrder)
        see(Part2VaultStrings.scanOrder).assertIsNotEnabled()

        scanner.gate?.complete(Unit)
        settle()
        see(Part2VaultStrings.orderScreenTitle).assertIsDisplayed()
    }

    @Test
    fun ordersTheDeviceCannotReadAreSaidAndTheOthersStillListed() {
        heldOrder("o1")
        repository.unreadableOrders = 2
        show()
        openPartTwoHome()

        see(Part2VaultStrings.vaultUnreadable).assertIsDisplayed()
        see(homeRow()).assertIsDisplayed()
    }

    @Test
    fun anOfficerWhoOnlyHoldsOrdersTheDeviceCannotReadIsStillTold() {
        repository.unreadableOrders = 1
        show(part2Unlocked = false)
        openPartTwoHome()
        see(Part2VaultStrings.vaultUnreadable).assertIsDisplayed()
        assertFalse(has(Part2VaultStrings.scanOrder))
    }
}
