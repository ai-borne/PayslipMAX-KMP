package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: every way a scan can end needs words the officer can act on (plan Phase 11 item 23). A scan that needs a connection once, one that
 * broke, a full order, a full vault and a partly saved scan are different next steps, so each gets its own sentence; a cancelled scan
 * says nothing because the officer chose it.
 */
class VaultNoticesTest {
    private fun saved(
        saved: Int,
        notSaved: Int = 0,
        refusal: Part2WriteResult? = null,
    ) = VaultNotices.of(CaptureResult.Saved(saved, notSaved, refusal))

    @Test
    fun aCancelledScanSaysNothing() {
        assertNull(VaultNotices.of(CaptureResult.Cancelled))
    }

    @Test
    fun aScannerThatNeedsAConnectionOnceSaysSoAndABrokenScanSaysItDidNotWork() {
        assertEquals(Part2VaultStrings.scanNeedsConnection, VaultNotices.of(CaptureResult.Failed(ScanFailure.UNAVAILABLE)))
        assertEquals(Part2VaultStrings.scanFailed, VaultNotices.of(CaptureResult.Failed(ScanFailure.FAILED)))
        assertTrue("connection once" in Part2VaultStrings.scanNeedsConnection, "the owner's wording (Round 19): needs a connection once")
    }

    @Test
    fun aFullOrderIsToldTheTenPageLimit() {
        assertEquals(Part2VaultStrings.orderFull(), VaultNotices.of(CaptureResult.OrderFull))
        assertTrue("10 pages" in Part2VaultStrings.orderFull())
    }

    @Test
    fun everyPageSavedSaysHowManyAndOnePageIsNotPlural() {
        assertEquals("2 pages saved.", saved(2))
        assertEquals("1 page saved.", saved(1))
    }

    @Test
    fun pagesThatCouldNotBeSavedAreCountedAfterTheOnesThatWere() {
        assertEquals("2 pages saved. 1 page could not be saved.", saved(2, notSaved = 1))
        assertEquals("1 page saved. 3 pages could not be saved.", saved(1, notSaved = 3))
        assertEquals("2 pages could not be saved.", saved(0, notSaved = 2))
    }

    @Test
    fun aFullVaultTellsTheOfficerToDeleteAnOrder() {
        assertEquals(Part2VaultStrings.vaultFull, saved(0, 1, Part2WriteResult.VAULT_FULL))
        assertEquals("1 page saved. ${Part2VaultStrings.vaultFull}", saved(1, 2, Part2WriteResult.VAULT_FULL), "the refusal explains the rest, so the count of unsaved pages is not added")
    }

    @Test
    fun anOrderThatFilledUpMidScanAndAnOrderThatIsGoneAreEachNamed() {
        assertEquals("1 page saved. ${Part2VaultStrings.orderHoldsMax()}", saved(1, 1, Part2WriteResult.TOO_MANY))
        assertEquals(Part2VaultStrings.orderGone, saved(0, 1, Part2WriteResult.NO_SUCH_ORDER))
    }

    @Test
    fun aRefusalWithNoSentenceOfItsOwnFallsBackToTheCountOfUnsavedPages() {
        assertEquals("1 page could not be saved.", saved(0, 1, Part2WriteResult.TOO_LARGE))
        assertEquals("1 page could not be saved.", saved(0, 1, Part2WriteResult.REJECTED))
    }

    @Test
    fun aScanThatSavedAndLostNothingHasNothingToSay() {
        assertNull(saved(0, 0))
    }
}
