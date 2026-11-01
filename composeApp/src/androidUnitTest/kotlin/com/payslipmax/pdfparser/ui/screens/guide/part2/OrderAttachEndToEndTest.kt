package com.payslipmax.pdfparser.ui.screens.guide.part2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.ChainCheck
import com.payslipmax.pdfparser.part2.domain.ChainState
import com.payslipmax.pdfparser.part2.domain.OrderCompleteness
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.AndroidPageEncoder
import com.payslipmax.pdfparser.part2.pages.AndroidPageScanner
import com.payslipmax.pdfparser.part2.pages.MlKitScanSource
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.part2.pages.SourceResult
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.JpegProbe
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.AttachOrderViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.AttachOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY: the promise of Phase 4 is end to end. A camera-tagged photo of an order goes through the real scanner folder rules, the real encoder,
 * the real encrypted repository and a `.pcda` backup; what a second phone holds is one small clean JPEG per page, the order with its count and
 * quoted order, and every ticked casualty attached to it, while the scanner's cache is empty. Each link is tested alone elsewhere; this proves
 * they fit together.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrderAttachEndToEndTest {
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

    private fun tagged(): ByteArray {
        val bitmap = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.rgb(250, 240, 200)) }
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        return JpegProbe.withMetadata(jpeg, orientation = 6, secret = "MARKER_GPS_TRACE")
    }

    private fun scanner(count: Int) =
        AndroidPageScanner(scanDir, { SourceResult.Files((1..count).map { i -> File(scanDir.also { it.mkdirs() }, "$i.jpg").also { f -> f.writeBytes(tagged()) }.path }) })

    @Test
    fun aScannedOrderComesOutOfABackupWithItsFieldsItsCasualtiesAndCleanPages() =
        runBlocking {
            val source = FakePayslipDao()
            val repository = RoomPart2Repository(dao = source.part2, dispatcher = Dispatchers.Unconfined)
            repository.saveOrder(SyntheticBank.order("older", orderNo = "SYN/OLDER"))
            listOf("e1", "e2", "e3").forEach { repository.saveEntry(SyntheticBank.entry(it)) }
            val vm = AttachOrderViewModel(repository, PageStore(AndroidPageEncoder(), repository, Dispatchers.Unconfined), { 9_000L }, { "order-new" }, Dispatchers.Unconfined)
            vm.begin()
            vm.scan(scanner(2))
            vm.toggle("e1")
            vm.toggle("e3")
            vm.setOrderNo("SYN/NEW")
            vm.setDate(CalendarDate(2026, 3, 20))
            vm.setExpected("3")
            vm.setLastOrderNo(" syn/older ")

            var outcome: AttachOutcome? = null
            vm.save { outcome = it }

            assertEquals(2 to 0, assertIs<AttachOutcome.Saved>(outcome).let { it.saved to it.notSaved })
            assertEquals(emptyList(), cache.walkTopDown().filter { it.isFile }.toList(), "no plaintext page is left anywhere under the app cache")

            val archive = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2)).export("pw-for-test").getOrThrow()
            val target = FakePayslipDao()
            PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2)).restore(archive, "pw-for-test", RestoreMode.REPLACE).getOrThrow()
            val second = RoomPart2Repository(dao = target.part2, dispatcher = Dispatchers.Unconfined)

            val order = assertNotNull(second.order("order-new"))
            assertEquals("SYN/NEW", order.orderNo)
            assertEquals(CalendarDate(2026, 3, 20), order.orderDate)
            assertEquals(3, order.expectedCount)
            assertEquals("syn/older", order.lastOrderNo)
            assertEquals(ChainState.FOUND, ChainCheck.of(order, listOf(order, assertNotNull(second.order("older")))))
            val orders = setOf("order-new", "older")
            assertEquals(listOf(BankStatus.ATTACHED, BankStatus.NOT_ATTACHED, BankStatus.ATTACHED), listOf("e1", "e2", "e3").map { BankStatus.of(assertNotNull(second.entry(it)), orders) })
            assertEquals(OrderCompleteness(2, 3), OrderCompleteness.of(order, listOf("e1", "e2", "e3").map { assertNotNull(second.entry(it)) }))

            val pages = second.pages("order-new").map { ref -> second.readPage(ref.id) { it.copyOf() }!! }
            assertEquals(2, pages.size)
            for (page in pages) {
                assertTrue(page.size <= Part2Limits.PAGE_BUDGET_BYTES, "${page.size} bytes")
                assertFalse(JpegProbe.contains(page, "MARKER_GPS_TRACE"))
                assertEquals(emptyList(), JpegProbe.markers(page).filter { it in 0xE1..0xED || it == 0xFE })
            }
        }
}
