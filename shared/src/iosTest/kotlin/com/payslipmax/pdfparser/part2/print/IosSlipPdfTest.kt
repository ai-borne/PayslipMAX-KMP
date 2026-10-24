package com.payslipmax.pdfparser.part2.print

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PDFKit.PDFDocument
import platform.posix.memcpy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * WHY: iOS prints the slip from PDF data held in memory, and the officer compares it with the preview, so the data must be a real PDF
 * with exactly the pages the layout made. The layout and the PDF run on every print on Kotlin/Native, where a slow string path has
 * already cost minutes once (the regex stall), so the whole path has a time bound.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSlipPdfTest {
    private fun document(rows: Int) =
        SlipDocument(
            title = "Slip title",
            lastOrder = "Last Part II order: ____",
            notice = "Draft notice",
            orders = listOf(SlipDocumentOrder("Order 1 · Group A", List(rows) { n -> SlipDocumentRow("CODE$n", "Narrative $n ".repeat(12).trim(), List(4) { "Detail $n.$it" }) })),
            pageLabel = "Page {page} of {pages}",
        )

    private fun bytes(data: NSData): ByteArray =
        ByteArray(data.length.toInt()).also { out -> if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }

    private fun pdfPages(data: NSData): Int = PDFDocument(data = data).pageCount.toInt()

    @Test
    fun aSmallSlipIsOnePagePdfData() {
        val data = IosSlipPdf.render(document(2))

        assertTrue(data.length > 0u, "the PDF data is not empty")
        assertEquals("%PDF-", bytes(data).copyOf(5).decodeToString())
        assertEquals(1, pdfPages(data))
        assertEquals(1, IosSlipPdf.layout(document(2)).size)
    }

    @Test
    fun aLongSlipIsAPdfWithExactlyTheLayoutsPages() {
        val layout = IosSlipPdf.layout(document(60))

        assertTrue(layout.size >= 3, "the slip must run to several pages: ${layout.size}")
        assertEquals(layout.size, pdfPages(IosSlipPdf.render(document(60))))
    }

    @Test
    fun theSheetsCompletedFlagIsTheOnlyThingThatCountsAsPrinted() {
        assertEquals(PrintOutcome.COMPLETED, IosSlipPrinter.outcomeOf(completed = true))
        assertEquals(PrintOutcome.NOT_PRINTED, IosSlipPrinter.outcomeOf(completed = false))
    }

    @Test
    fun theLargestSlipIsLaidOutAndRenderedWithinTheBudget() {
        // 10 requests, each with its linked orders: the most rows a draft can hold (Part2Limits.PRINTED_ROWS_MAX).
        val doc = document(170)
        val start = TimeSource.Monotonic.markNow()

        val data = IosSlipPdf.render(doc)

        val ms = start.elapsedNow().inWholeMilliseconds
        println("IosSlipPdf 170 rows: $ms ms, ${pdfPages(data)} pages")
        assertTrue(ms < BUDGET_MS, "$ms ms for the largest slip, budget $BUDGET_MS")
    }

    private companion object {
        const val BUDGET_MS = 1500L
    }
}
