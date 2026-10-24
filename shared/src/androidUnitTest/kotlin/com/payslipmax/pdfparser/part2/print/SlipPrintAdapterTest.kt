package com.payslipmax.pdfparser.part2.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the system print dialog asks the app for a layout (how many pages) and then for the pages themselves, once for the preview and
 * again for the print, possibly only some pages. What it is handed must be a valid PDF with exactly the pages asked for, and a
 * cancelled or broken write must leave nothing half written and say nothing about the slip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SlipPrintAdapterTest {
    private val pages = SlipPrintFixtures.pages
    private val documents = mutableListOf<RecordingPdfDocument>()
    private var failOnWrite = false
    private val adapter = SlipPrintAdapter("Job name", pages) { RecordingPdfDocument(failOnWrite).also { documents += it } }
    private lateinit var file: File

    @Before
    fun makeFile() {
        file = File.createTempFile("slip-test", ".pdf")
    }

    @After
    fun removeFile() {
        file.delete()
    }

    private fun destination() = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)

    private fun write(
        ranges: Array<PageRange>,
        signal: CancellationSignal? = null,
        callback: PrintDocumentAdapter.WriteResultCallback = mockk(relaxed = true),
    ): PrintDocumentAdapter.WriteResultCallback {
        adapter.onWrite(ranges, destination(), signal, callback)
        return callback
    }

    @Test
    fun theLayoutNamesTheJobAndCountsEveryPage() {
        assertTrue(pages.size >= 3, "the fixture must be several pages")
        val callback = mockk<PrintDocumentAdapter.LayoutResultCallback>(relaxed = true)
        val info = slot<PrintDocumentInfo>()

        adapter.onLayout(null, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(), null, callback, null)

        verify { callback.onLayoutFinished(capture(info), true) }
        assertEquals("Job name", info.captured.name)
        assertEquals(pages.size, info.captured.pageCount)
        assertEquals(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT, info.captured.contentType)
    }

    @Test
    fun aCancelledLayoutIsReportedCancelled() {
        val signal = CancellationSignal().apply { cancel() }
        val callback = mockk<PrintDocumentAdapter.LayoutResultCallback>(relaxed = true)

        adapter.onLayout(null, PrintAttributes.Builder().build(), signal, callback, null)

        verify { callback.onLayoutCancelled() }
        verify(exactly = 0) { callback.onLayoutFinished(any(), any()) }
    }

    @Test
    fun allPagesAreWrittenOnePageEachInOrderToTheDestinationAndTheDocumentIsClosed() {
        val callback = write(arrayOf(PageRange.ALL_PAGES))

        val document = documents.single()
        assertEquals((1..pages.size).toList(), document.started)
        assertEquals(pages.size, document.finished)
        assertEquals(1, document.written)
        assertTrue(document.closed)
        assertEquals(RecordingPdfDocument.MARK.decodeToString(), file.readText(), "the PDF goes to the destination the system gave, and nowhere else")
        verify { callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) }
    }

    @Test
    fun eachPageIsDrawnFromItsOwnLaidOutPage() {
        write(arrayOf(PageRange.ALL_PAGES))

        documents.single().canvases.forEachIndexed { i, canvas ->
            verify { canvas.drawText(pages[i].texts.first().text, any(), any(), any()) }
        }
    }

    @Test
    fun aRangeWritesOnlyThosePagesRenumberedFromOneAndSaysSo() {
        val range = PageRange(1, 2)
        val callback = write(arrayOf(range))

        val document = documents.single()
        assertEquals(listOf(1, 2), document.started)
        verify { document.canvases[0].drawText(pages[1].texts.first().text, any(), any(), any()) }
        verify { document.canvases[1].drawText(pages[2].texts.first().text, any(), any(), any()) }
        verify { callback.onWriteFinished(arrayOf(range)) }
    }

    @Test
    fun twoRangesThatOverlapWriteEachPageOnce() {
        write(arrayOf(PageRange(0, 1), PageRange(1, 2)))

        assertEquals(listOf(1, 2, 3), documents.single().started)
    }

    @Test
    fun rangesGivenOutOfOrderAreStillWrittenInPageOrder() {
        write(arrayOf(PageRange(2, 2), PageRange(0, 0)))

        val document = documents.single()
        verify { document.canvases[0].drawText(pages[0].texts.first().text, any(), any(), any()) }
        verify { document.canvases[1].drawText(pages[2].texts.first().text, any(), any(), any()) }
    }

    @Test
    fun aRangePastTheLastPageIsClippedToThePagesThereAre() {
        write(arrayOf(PageRange(pages.size - 1, pages.size + 40)))

        assertEquals(listOf(1), documents.single().started)
    }

    @Test
    fun aCancelledWriteMakesNoDocumentWritesNothingAndSaysCancelled() {
        val signal = CancellationSignal().apply { cancel() }
        val callback = write(arrayOf(PageRange.ALL_PAGES), signal)

        assertTrue(documents.isEmpty())
        assertEquals(0, file.length())
        verify { callback.onWriteCancelled() }
        verify(exactly = 0) { callback.onWriteFinished(any()) }
    }

    @Test
    fun aDestinationThatCannotBeWrittenFailsTheWriteWithNoMessage() {
        val callback = mockk<PrintDocumentAdapter.WriteResultCallback>(relaxed = true)
        val closed = destination().also { it.close() }

        adapter.onWrite(arrayOf(PageRange.ALL_PAGES), closed, null, callback)

        verify { callback.onWriteFailed(null) }
        verify(exactly = 0) { callback.onWriteFinished(any()) }
        assertTrue(documents.single().closed)
    }

    @Test
    fun aDocumentThatFailsWhileWritingFailsTheWriteWithNoMessageAndIsStillClosed() {
        failOnWrite = true
        val callback = write(arrayOf(PageRange.ALL_PAGES))

        verify { callback.onWriteFailed(null) }
        assertTrue(documents.single().closed)
    }
}
