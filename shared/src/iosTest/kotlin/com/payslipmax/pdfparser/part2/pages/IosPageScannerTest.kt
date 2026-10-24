package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.crypto.toNSData
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: on iOS the document camera hands pages over in memory (nothing is written to disk), through a Swift presenter. This class turns
 * the presenter's answer into pages, keeps only as many as the order has room for, says plainly when the camera cannot run, and closes
 * the camera when the officer's screen goes away. Like the Android scanner it never lets a page's content into its text.
 */
class IosPageScannerTest {
    private class FakeDelegate(private val answer: (IosScanListener) -> Unit) : IosDocumentScannerDelegate {
        val asked = mutableListOf<Int>()
        var dismissed = 0
        var listener: IosScanListener? = null

        override fun present(
            maxPages: Int,
            listener: IosScanListener,
        ) {
            asked += maxPages
            this.listener = listener
            answer(listener)
        }

        override fun dismiss() {
            dismissed++
        }
    }

    private fun scanner(delegate: IosDocumentScannerDelegate?) = IosPageScanner(Dispatchers.Unconfined) { delegate }

    private fun data(vararg bytes: Byte) = bytes.toNSData()

    @Test
    fun thePagesThePresenterReturnsBecomePagesInOrder() =
        runTest {
            val delegate = FakeDelegate { it.onPages(listOf(data(1, 2), data(3))) }

            val pages = assertIs<PageScan.Pages>(scanner(delegate).scan(7))

            assertEquals(listOf(7), delegate.asked, "the presenter is told how many pages the order has room for")
            assertContentEquals(byteArrayOf(1, 2), pages.pages[0].read())
            assertContentEquals(byteArrayOf(3), pages.pages[1].read())
            assertEquals(0, pages.dropped)
        }

    @Test
    fun morePagesThanTheRoomLeftAreDroppedAndCounted() =
        runTest {
            val delegate = FakeDelegate { it.onPages(List(5) { n -> data(n.toByte()) }) }

            val pages = assertIs<PageScan.Pages>(scanner(delegate).scan(3))

            assertEquals(3, pages.pages.size)
            assertEquals(2, pages.dropped)
            assertContentEquals(byteArrayOf(2), pages.pages[2].read(), "the first pages are kept, the last ones dropped")
        }

    @Test
    fun noPagesOrACloseIsACancel() =
        runTest {
            assertEquals(PageScan.Cancelled, scanner(FakeDelegate { it.onPages(emptyList()) }).scan(10))
            assertEquals(PageScan.Cancelled, scanner(FakeDelegate { it.onCancelled() }).scan(10))
        }

    @Test
    fun aCameraThatCannotRunSaysWhy() =
        runTest {
            assertEquals(ScanFailure.UNAVAILABLE, assertIs<PageScan.Failed>(scanner(FakeDelegate { it.onUnavailable() }).scan(10)).reason)
            assertEquals(ScanFailure.FAILED, assertIs<PageScan.Failed>(scanner(FakeDelegate { it.onFailed() }).scan(10)).reason)
        }

    @Test
    fun withNoPresenterRegisteredTheCameraIsUnavailable() =
        runTest {
            assertEquals(ScanFailure.UNAVAILABLE, assertIs<PageScan.Failed>(scanner(null).scan(10)).reason)
        }

    @Test
    fun aSecondAnswerFromThePresenterIsIgnored() =
        runTest {
            val delegate =
                FakeDelegate {
                    it.onPages(listOf(data(1)))
                    it.onCancelled()
                    it.onPages(listOf(data(2), data(3)))
                }

            val pages = assertIs<PageScan.Pages>(scanner(delegate).scan(10))

            assertEquals(1, pages.pages.size)
        }

    @Test
    fun theCameraIsShownOnTheDispatcherItWasGiven() =
        runTest {
            var insideUi = false
            var shownInsideUi = false
            val ui =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        insideUi = true
                        block.run()
                        insideUi = false
                    }
                }
            val delegate =
                FakeDelegate {
                    shownInsideUi = insideUi
                    it.onCancelled()
                }

            IosPageScanner(ui) { delegate }.scan(3)

            assertTrue(shownInsideUi, "UIKit must be driven from the main dispatcher")
        }

    @Test
    fun walkingAwayWhileTheCameraIsOpenClosesIt() =
        runTest {
            val delegate = FakeDelegate { }
            val job = launch(start = CoroutineStart.UNDISPATCHED) { scanner(delegate).scan(10) }

            job.cancelAndJoin()

            assertEquals(1, delegate.dismissed)
        }

    @Test
    fun aFinishedScanDoesNotCloseTheCameraAgain() =
        runTest {
            val delegate = FakeDelegate { it.onCancelled() }

            scanner(delegate).scan(10)

            assertEquals(0, delegate.dismissed)
        }

    @Test
    fun aDiscardedPageCannotBeReadAndDiscardingTwiceIsHarmless() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(FakeDelegate { it.onPages(listOf(data(1))) }).scan(10))
            val page = pages.pages.single()

            page.discard()
            page.discard()

            assertNull(page.read())
        }

    @Test
    fun aPageLargerThanTheReadCapIsNotCopiedIntoMemory() =
        runTest {
            val big = ByteArray(Part2Limits.PAGE_RAW_MAX_BYTES + 1).toNSData()
            val pages = assertIs<PageScan.Pages>(scanner(FakeDelegate { it.onPages(listOf(big)) }).scan(10))

            assertNull(pages.pages.single().read())
        }

    @Test
    fun nothingIsLeftOnDiskSoThereIsNothingToSweep() {
        scanner(null).discardLeftovers()
    }

    @Test
    fun aPageNeverPrintsItsContent() =
        runTest {
            val pages = assertIs<PageScan.Pages>(scanner(FakeDelegate { it.onPages(listOf("MARKER_page".encodeToByteArray().toNSData())) }).scan(10))

            assertFalse("MARKER" in pages.toString() + pages.pages.single().toString())
            assertTrue(pages.pages.isNotEmpty())
        }
}
