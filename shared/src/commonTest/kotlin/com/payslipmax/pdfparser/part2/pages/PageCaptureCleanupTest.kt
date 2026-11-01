package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.AddedPage
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY: the same promise as `PageCaptureTest`, under failure. Whatever goes wrong between the scanner's answer and the vault (an encoder
 * that refuses or throws, a vault that is full, an order that is gone, an officer who walks away mid-page), the scanner's copy of every
 * page is deleted and the plaintext bytes are overwritten.
 */
internal class PageCaptureCleanupTest : PageCaptureFixture() {
    @Test
    fun theRawBytesAreOverwrittenAfterTheEncoderAndTheStoredBytesAfterTheWrite() =
        runTest {
            order()
            scanner.next = PageScan.Pages(listOf(page(5, 6, 7)))
            val handedToRepository = mutableListOf<ByteArray>()
            val spying =
                object : Part2Repository by repository {
                    override suspend fun addPage(
                        orderId: String,
                        jpeg: ByteArray,
                    ): AddedPage = repository.addPage(orderId, jpeg).also { handedToRepository += jpeg }
                }

            capture(repository = spying).capture("o1")

            assertContentEquals(byteArrayOf(0, 0, 0), encoder.received.single(), "the plaintext the scanner produced is zeroed once encoded")
            assertContentEquals(ByteArray(4), handedToRepository.single(), "the bytes handed to the repository are zeroed once stored")
        }

    @Test
    fun theBytesAreOverwrittenEvenWhenTheEncoderRefusesThem() =
        runTest {
            order()
            scanner.next = PageScan.Pages(listOf(page(5, 6, 7)))
            val refusing = RecordingEncoder(refuse = { true })

            capture(refusing).capture("o1")

            assertContentEquals(byteArrayOf(0, 0, 0), refusing.received.single())
        }

    @Test
    fun anEncoderThatThrowsLosesThatPageOnlyAndNothingIsLeftBehind() =
        runTest {
            order()
            val pages = listOf(page(1), page(2, 2), page(3))
            scanner.next = PageScan.Pages(pages)
            val exploding = RecordingEncoder(explode = { it.size == 2 })

            val saved = assertIs<CaptureResult.Saved>(capture(exploding).capture("o1"))

            assertEquals(2 to 1, saved.saved to saved.notSaved)
            assertTrue(pages.all { it.discarded })
            assertContentEquals(ByteArray(2), exploding.received[1], "the bytes of the page that broke the encoder are overwritten too")
        }

    @Test
    fun whenTheRepositoryRefusesTheRestAreNotEncodedButAreAllDiscarded() =
        runTest {
            order()
            repeat(Part2Limits.PAGES_PER_ORDER_MAX - 1) { repository.addPage("o1", byteArrayOf(9)) }
            val pages = listOf(page(1), page(2), page(3))
            scanner.next = PageScan.Pages(pages)

            val saved = assertIs<CaptureResult.Saved>(capture().capture("o1"))

            assertEquals(1 to 2, saved.saved to saved.notSaved)
            assertEquals(Part2WriteResult.TOO_MANY, saved.refusal)
            assertEquals(2, encoder.received.size, "the page after the refusal is not worth encoding")
            assertTrue(pages.all { it.discarded })
        }

    @Test
    fun aMissingOrderIsReportedAsARefusalAndNothingIsKept() =
        runTest {
            val pages = listOf(page(1), page(2))
            scanner.next = PageScan.Pages(pages)

            val saved = assertIs<CaptureResult.Saved>(capture().capture("nope"))

            assertEquals(0 to 2, saved.saved to saved.notSaved)
            assertEquals(Part2WriteResult.NO_SUCH_ORDER, saved.refusal)
            assertTrue(pages.all { it.discarded })
        }

    @Test
    fun cancellingWhileAPageIsBeingStoredStillDeletesEveryPageTheScannerLeft() =
        runTest {
            order()
            val pages = listOf(page(1), page(2), page(3))
            scanner.next = PageScan.Pages(pages)
            val entered = CompletableDeferred<Unit>()
            val never = CompletableDeferred<Unit>()
            val stuck =
                object : Part2Repository by repository {
                    override suspend fun addPage(
                        orderId: String,
                        jpeg: ByteArray,
                    ): AddedPage {
                        entered.complete(Unit)
                        never.await()
                        return repository.addPage(orderId, jpeg)
                    }
                }

            val job = launch(start = CoroutineStart.UNDISPATCHED) { capture(repository = stuck).capture("o1") }
            entered.await()
            job.cancelAndJoin()

            assertTrue(pages.all { it.discarded }, "a scan the officer walks away from must not leave plaintext pages in the cache")
        }

    /** Holds each dispatched block until the test runs it, so the test can cancel while a page is being read or encoded. */
    private class HeldDispatcher : CoroutineDispatcher() {
        val held = ArrayDeque<Runnable>()

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            held.addLast(block)
        }

        fun runNext() = held.removeFirst().run()
    }

    @Test
    fun walkingAwayWhileAPageIsBeingEncodedStopsTheCaptureAndDeletesEveryPage() =
        runTest {
            order()
            val pages = listOf(page(1))
            scanner.next = PageScan.Pages(pages)
            val held = HeldDispatcher()
            var outcome: Result<CaptureResult>? = null
            val job = launch(start = CoroutineStart.UNDISPATCHED) { outcome = runCatching { PageCapture(scanner, encoder, repository, held).capture("o1") } }
            held.runNext()
            testScheduler.runCurrent()
            assertEquals(1, held.held.size, "the first page is now waiting to be encoded")

            job.cancel()
            held.runNext()
            testScheduler.runCurrent()
            job.join()

            assertTrue(outcome!!.exceptionOrNull() is CancellationException, "a cancelled capture ends cancelled; it does not report the page as merely unsaved: $outcome")
            assertTrue(pages.all { it.discarded })
            assertTrue(repository.pages("o1").isEmpty())
        }
}
