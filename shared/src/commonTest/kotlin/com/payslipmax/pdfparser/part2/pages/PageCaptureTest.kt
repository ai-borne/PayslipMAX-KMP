package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a scanned page is a photo of a RESTRICTED order. Between the scanner's answer and the vault it exists as plaintext, so the one
 * place that moves it must guarantee, whatever goes wrong, that the scanner's copy is deleted, the plaintext bytes are overwritten and
 * the officer is told how many pages were kept. The view model of Phase 6b only depends on this class.
 */
internal class PageCaptureTest : PageCaptureFixture() {
    @Test
    fun everyPageIsEncodedStoredInOrderAndThenDiscarded() =
        runTest {
            order()
            val pages = listOf(page(1), page(2), page(3))
            scanner.next = PageScan.Pages(pages)

            val result = capture().capture("o1")

            val saved = assertIs<CaptureResult.Saved>(result)
            assertEquals(3 to 0, saved.saved to saved.notSaved)
            assertNull(saved.refusal)
            val stored = repository.pages("o1").map { ref -> repository.readPage(ref.id) { it.copyOf() } }
            assertEquals(listOf(listOf<Byte>(0x7E, 1), listOf<Byte>(0x7E, 2), listOf<Byte>(0x7E, 3)), stored.map { it!!.toList() })
            assertTrue(pages.all { it.discarded }, "the scanner's copy of every page is deleted")
        }

    @Test
    fun aPageIsDiscardedBeforeTheNextOneIsReadSoNoMoreThanOnePlaintextPageIsHeld() =
        runTest {
            order()
            val log = mutableListOf<String>()
            scanner.next = PageScan.Pages(listOf(page(1, log = log, name = "a"), page(2, log = log, name = "b")))

            capture().capture("o1")

            assertEquals(listOf("read a", "discard a", "read b", "discard b"), log)
        }

    @Test
    fun theScannerIsAskedForOnlyTheRoomThatIsLeftInTheOrder() =
        runTest {
            order()
            repeat(3) { repository.addPage("o1", byteArrayOf(9)) }
            scanner.next = PageScan.Cancelled

            capture().capture("o1")

            assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX - 3), scanner.limits)
        }

    @Test
    fun anOrderThatIsFullIsNeverScanned() =
        runTest {
            order()
            repeat(Part2Limits.PAGES_PER_ORDER_MAX) { repository.addPage("o1", byteArrayOf(9)) }

            val result = capture().capture("o1")

            assertEquals(CaptureResult.OrderFull, result)
            assertTrue(scanner.limits.isEmpty(), "the camera must not open for pages that cannot be kept")
        }

    @Test
    fun pagesGoToTheOrderThatWasAskedFor() =
        runTest {
            order("other-order")

            scanner.next = PageScan.Pages(listOf(page(1)))
            capture().capture("other-order")

            assertEquals(1, repository.pages("other-order").size)
        }

    @Test
    fun aClosedScannerSavesNothing() =
        runTest {
            order()
            scanner.next = PageScan.Cancelled

            assertEquals(CaptureResult.Cancelled, capture().capture("o1"))
            assertTrue(repository.pages("o1").isEmpty())
        }

    @Test
    fun aScannerThatCannotRunSaysWhy() =
        runTest {
            order()
            for (reason in ScanFailure.entries) {
                scanner.next = PageScan.Failed(reason)

                assertEquals(reason, assertIs<CaptureResult.Failed>(capture().capture("o1")).reason)
            }
        }

    @Test
    fun aPageThatCannotBeReadIsCountedAndTheOthersAreKept() =
        runTest {
            order()
            val gone = page(2).also { it.readable = false }
            val pages = listOf(page(1), gone, page(3))
            scanner.next = PageScan.Pages(pages)

            val saved = assertIs<CaptureResult.Saved>(capture().capture("o1"))

            assertEquals(2 to 1, saved.saved to saved.notSaved)
            assertTrue(pages.all { it.discarded }, "an unreadable page's leftover is deleted too")
        }

    @Test
    fun aPageTheEncoderRefusesIsCountedAndDiscarded() =
        runTest {
            order()
            val pages = listOf(page(1), page(0, 0), page(3))
            scanner.next = PageScan.Pages(pages)

            val saved = assertIs<CaptureResult.Saved>(capture(RecordingEncoder(refuse = { it.size == 2 })).capture("o1"))

            assertEquals(2 to 1, saved.saved to saved.notSaved)
            assertTrue(pages.all { it.discarded })
        }

    @Test
    fun pagesTheScannerDroppedAreCountedAsNotSaved() =
        runTest {
            order()
            scanner.next = PageScan.Pages(listOf(page(1)), dropped = 2)

            val saved = assertIs<CaptureResult.Saved>(capture().capture("o1"))

            assertEquals(1 to 2, saved.saved to saved.notSaved)
        }

    @Test
    fun theCaptureResultNeverPrintsWhatWasScanned() {
        assertTrue("MARKER" !in PageScan.Pages(listOf(page(1))).toString())
    }
}
