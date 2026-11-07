package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.JpegProbe
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY: the promise of Phase 6a is end to end. A photo of an order, tagged by the camera with where and when it was taken, goes in; what a
 * second device holds after a backup and restore is one small upright JPEG with none of that, and the scanner's cache is empty at every
 * exit. Each link is tested alone elsewhere; this proves they fit together with the real codec, crypto, repository and backup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageChainAndroidTest {
    private lateinit var cache: File
    private lateinit var scanDir: File

    @Before
    fun setUp() {
        cache = Files.createTempDirectory("MARKER_cache").toFile()
        scanDir = File(cache, MlKitScanSource.OUTPUT_DIR)
    }

    @After
    fun tearDown() {
        cache.deleteRecursively()
    }

    private fun tagged(
        width: Int,
        height: Int,
        orientation: Int = 1,
    ) = JpegProbe.withMetadata(PageBitmaps.jpeg(PageBitmaps.noisy(width, height, 50)), orientation, "MARKER_GPS_TRACE")

    @Test
    fun scannedTaggedPhotosComeOutOfABackupAsSmallCleanUprightPages() =
        runTest {
            val raw = listOf(tagged(3000, 2000), tagged(2000, 3000, orientation = 6), tagged(1000, 700))

            val trip = roundTrip(AndroidPageEncoder(), raw)

            val saved = assertIs<CaptureResult.Saved>(trip.captured)
            assertEquals(3 to 0, saved.saved to saved.notSaved)
            assertEquals(3, trip.afterRestore.size)
            for ((stored, restored) in trip.onDevice.zip(trip.afterRestore)) {
                assertContentEquals(stored, restored, "the restored page is the stored page, byte for byte")
                assertTrue(restored.size <= Part2Limits.PAGE_MAX_BYTES)
                assertFalse(JpegProbe.contains(restored, "MARKER_GPS_TRACE"))
                assertEquals(emptyList(), JpegProbe.markers(restored).filter { it in 0xE1..0xED || it == 0xFE })
            }
            assertEquals(listOf(1600 to 1067, 1600 to 1067, 1000 to 700), trip.afterRestore.map { JpegProbe.size(it) })
        }

    private fun scanner(vararg pages: Pair<String, ByteArray>) =
        AndroidPageScanner(scanDir, { SourceResult.Files(pages.map { (name, bytes) -> File(scanDir.also { it.mkdirs() }, name).also { f -> f.writeBytes(bytes) }.path }) })

    private suspend fun capture(
        scanner: PageScanner,
        repository: RoomPart2Repository,
    ) = PageCapture(scanner, AndroidPageEncoder(), repository, Dispatchers.Unconfined).capture("o1")

    private suspend fun repositoryWithOrder() = RoomPart2Repository(dao = FakePayslipDao().part2, dispatcher = Dispatchers.Unconfined).also { it.saveOrder(SyntheticBank.order("o1")) }

    @Test
    fun theScannersCacheIsEmptyAfterASuccessfulCapture() =
        runTest {
            val repository = repositoryWithOrder()

            val result = capture(scanner("1.jpg" to tagged(1200, 800), "2.jpg" to tagged(1200, 800)), repository)

            assertEquals(2, assertIs<CaptureResult.Saved>(result).saved)
            assertEquals(emptyList(), scanDir.listFiles().orEmpty().toList())
        }

    @Test
    fun theScannersCacheIsEmptyWhenAPageCannotBeEncoded() =
        runTest {
            val repository = repositoryWithOrder()

            val result = capture(scanner("1.jpg" to tagged(1200, 800), "2.jpg" to "MARKER_not_an_image".encodeToByteArray(), "3.jpg" to tagged(900, 600)), repository)

            val saved = assertIs<CaptureResult.Saved>(result)
            assertEquals(2 to 1, saved.saved to saved.notSaved)
            assertEquals(emptyList(), scanDir.listFiles().orEmpty().toList(), "the unreadable page's file is deleted too")
        }

    @Test
    fun theScannersCacheIsEmptyWhenTheVaultRefusesAndWhenTheOrderIsGone() =
        runTest {
            val full = repositoryWithOrder().also { repository -> repeat(Part2Limits.PAGES_PER_ORDER_MAX - 1) { repository.addPage("o1", byteArrayOf(1)) } }

            capture(scanner("1.jpg" to tagged(900, 600), "2.jpg" to tagged(900, 600), "3.jpg" to tagged(900, 600)), full)
            assertEquals(emptyList(), scanDir.listFiles().orEmpty().toList(), "vault full")

            capture(scanner("4.jpg" to tagged(900, 600)), RoomPart2Repository(dao = FakePayslipDao().part2, dispatcher = Dispatchers.Unconfined))
            assertEquals(emptyList(), scanDir.listFiles().orEmpty().toList(), "order gone")
        }

    @Test
    fun theScannersCacheIsEmptyWhenTheOfficerWalksAwayWhileAPageIsBeingStored() =
        runTest {
            val real = repositoryWithOrder()
            val entered = CompletableDeferred<Unit>()
            val never = CompletableDeferred<Unit>()
            val stuck =
                object : com.payslipmax.pdfparser.part2.data.Part2Repository by real {
                    override suspend fun addPage(
                        orderId: String,
                        jpeg: ByteArray,
                    ): com.payslipmax.pdfparser.part2.data.AddedPage {
                        entered.complete(Unit)
                        never.await()
                        return real.addPage(orderId, jpeg)
                    }
                }

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    PageCapture(scanner("1.jpg" to tagged(900, 600), "2.jpg" to tagged(900, 600)), AndroidPageEncoder(), stuck, Dispatchers.Unconfined).capture("o1")
                }
            entered.await()
            job.cancelAndJoin()

            assertEquals(emptyList(), scanDir.listFiles().orEmpty().toList())
        }

    @Test
    fun noPlaintextPageIsLeftAnywhereUnderTheCacheOrItsTempAfterwards() =
        runTest {
            val repository = repositoryWithOrder()
            capture(scanner("1.jpg" to tagged(1200, 800)), repository)

            val survivors = cache.walkTopDown().filter { it.isFile }.toList()

            assertEquals(emptyList(), survivors, "no file at all remains under the app cache")
        }
}
