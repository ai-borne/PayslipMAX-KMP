package com.payslipmax.pdfparser.part2.pages

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * WHY: the attach flow lets the officer scan more pages before saving, so pages handed over by an earlier scan are still in use. A later
 * scan, a cancelled or failed one, and the sweep at the Bank must delete what nobody holds (a killed scan's leftovers) and never the pages
 * the app is still holding, or "Scan more pages" would silently lose the first page. Found on the Pixel walk.
 */
class AndroidPageScannerHeldPagesTest : ScanDirFixture() {
    private suspend fun heldFirst(scanner: AndroidPageScanner): ScannedPage {
        val first = assertIs<PageScan.Pages>(scanner.scan(10)).pages.single()
        return first
    }

    @Test
    fun aSecondScanKeepsThePagesTheFirstOneHandedOver() =
        runTest {
            var round = 0
            val scanner = scanner { if (++round == 1) writing("first.jpg" to byteArrayOf(1, 1)).scan(10) else writing("second.jpg" to byteArrayOf(2, 2)).scan(10) }
            val first = heldFirst(scanner)

            val second = assertIs<PageScan.Pages>(scanner.scan(9)).pages.single()

            assertContentEquals(byteArrayOf(1, 1), first.read(), "the first page is still there to be stored")
            assertContentEquals(byteArrayOf(2, 2), second.read())
            assertEquals(listOf("first.jpg", "second.jpg"), left().sorted())
        }

    @Test
    fun aCancelledOrFailedSecondScanLeavesTheHeldPageAlone() =
        runTest {
            var round = 0
            val scanner =
                scanner {
                    if (++round == 1) {
                        writing("first.jpg" to byteArrayOf(1)).scan(10)
                    } else if (round == 2) {
                        SourceResult.Cancelled
                    } else {
                        SourceResult.Failed(ScanFailure.FAILED)
                    }
                }
            val first = heldFirst(scanner)

            scanner.scan(9)
            scanner.scan(9)

            assertContentEquals(byteArrayOf(1), first.read())
            assertEquals(listOf("first.jpg"), left())
        }

    @Test
    fun theSweepDeletesStraysButNotAHeldPage() =
        runTest {
            val scanner = scanner(writing("held.jpg" to byteArrayOf(1)))
            val held = heldFirst(scanner)
            scanned("stray.jpg", 9)

            scanner.discardLeftovers()

            assertEquals(listOf("held.jpg"), left())
            held.discard()
            scanner.discardLeftovers()
            assertEquals(emptyList(), left())
        }

    @Test
    fun aDiscardedPageIsNoLongerHeldSoTheNextSweepRemovesAnythingLeft() =
        runTest {
            val scanner = scanner(writing("p.jpg" to byteArrayOf(1)))
            val page = heldFirst(scanner)

            page.discard()
            scanned("p.jpg", 5)
            scanner.discardLeftovers()

            assertEquals(emptyList(), left())
        }
}
