package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * WHY: the officer must be told in words, and the right words, how a scan or a save ended: a closed scanner is no error, a scanner that could
 * not start is not a scan that broke, dropped pages are counted and a full vault is not "something went wrong".
 */
class OrderNoticesTest {
    @Test
    fun everyScanNoticeHasItsOwnSentence() {
        val all = AttachNotice.entries.map(OrderNotices::of)

        assertEquals(all.size, all.toSet().size)
        assertEquals(OrderStrings.scanUnavailable, OrderNotices.of(AttachNotice.SCAN_UNAVAILABLE))
        assertEquals(OrderStrings.scanFailed, OrderNotices.of(AttachNotice.SCAN_FAILED))
        assertEquals(OrderStrings.orderFull(), OrderNotices.of(AttachNotice.ORDER_FULL))
    }

    @Test
    fun aClosedScannerAndASuccessfulScanSayNothing() {
        assertNull(OrderNotices.of(null as CaptureResult?))
        assertNull(OrderNotices.of(CaptureResult.Cancelled))
        assertNull(OrderNotices.of(CaptureResult.Saved(3, 0)))
    }

    @Test
    fun theCaptureFailuresAreToldApart() {
        val unavailable = OrderNotices.of(CaptureResult.Failed(ScanFailure.UNAVAILABLE))
        val broken = OrderNotices.of(CaptureResult.Failed(ScanFailure.FAILED))

        assertEquals(OrderStrings.scanUnavailable, unavailable)
        assertEquals(OrderStrings.scanFailed, broken)
        assertNotEquals(unavailable, broken)
        assertEquals(OrderStrings.orderFull(), OrderNotices.of(CaptureResult.OrderFull))
    }

    @Test
    fun droppedPagesAreCountedAndAFullVaultOrAGoneOrderHasItsOwnWords() {
        assertEquals(OrderStrings.pagesSaved(2, 5), OrderNotices.of(CaptureResult.Saved(2, 3)))
        assertEquals(OrderStrings.vaultFull, OrderNotices.of(CaptureResult.Saved(0, 2, Part2WriteResult.VAULT_FULL)))
        assertEquals(OrderStrings.orderGone, OrderNotices.of(CaptureResult.Saved(0, 2, Part2WriteResult.NO_SUCH_ORDER)))
    }

    @Test
    fun aSaveThatDidNotFinishNamesWhyAndAPartialOneCountsThePages() {
        assertNull(OrderNotices.outcome(AttachOutcome.Invalid), "the form's own problems are shown beside the fields")
        assertEquals(OrderStrings.bankFull, OrderNotices.outcome(AttachOutcome.BankFull))
        assertEquals(OrderStrings.vaultFull, OrderNotices.outcome(AttachOutcome.VaultFull))
        assertEquals(OrderStrings.saveFailed, OrderNotices.outcome(AttachOutcome.Failed))
        assertNull(OrderNotices.outcome(AttachOutcome.Saved("o", 2, 0)))
        assertEquals(OrderStrings.pagesSaved(1, 3), OrderNotices.outcome(AttachOutcome.Saved("o", 1, 2)))
    }

    @Test
    fun everyFormProblemHasWordsAndTheCounterNeverReadsOfQuestionMark() {
        assertEquals(AttachProblem.entries.size, AttachProblem.entries.map(OrderNotices::problem).count { it.isNotBlank() })
        assertEquals("3 of 5 casualties", OrderStrings.counter(com.payslipmax.pdfparser.part2.domain.OrderCompleteness(3, 5)))
        assertEquals("3 casualties", OrderStrings.counter(com.payslipmax.pdfparser.part2.domain.OrderCompleteness(3, null)))
        assertEquals("1 casualty", OrderStrings.counter(com.payslipmax.pdfparser.part2.domain.OrderCompleteness(1, null)))
    }
}
