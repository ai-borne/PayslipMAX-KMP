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
 * WHY: iOS prints the index from PDF data held in memory, and the officer compares it with the Bank, so the data must be a real PDF
 * with exactly the pages the layout made. The layout and the PDF run on every print on Kotlin/Native, where a slow string path has
 * already cost minutes once (the regex stall), so the whole path has a time bound.
 */
@OptIn(ExperimentalForeignApi::class)
class IosIndexPdfTest {
    private fun document(rows: Int) = IndexFixtures.document(listOf(IndexFixtures.year("2026", rows)))

    private fun bytes(data: NSData): ByteArray =
        ByteArray(data.length.toInt()).also { out -> if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }

    private fun pdfPages(data: NSData): Int = PDFDocument(data = data).pageCount.toInt()

    @Test
    fun aSmallIndexIsOnePagePdfData() {
        val data = IosIndexPdf.render(document(2))!!

        assertTrue(data.length > 0u, "the PDF data is not empty")
        assertEquals("%PDF-", bytes(data).copyOf(5).decodeToString())
        assertEquals(1, pdfPages(data))
        assertEquals(1, IosIndexPdf.layout(document(2)).size)
    }

    @Test
    fun aLongIndexIsAPdfWithExactlyTheLayoutsPages() {
        val layout = IosIndexPdf.layout(document(60))

        assertTrue(layout.size >= 3, "the index must run to several pages: ${layout.size}")
        assertEquals(layout.size, pdfPages(IosIndexPdf.render(document(60))!!))
    }

    @Test
    fun theSheetsCompletedFlagIsTheOnlyThingThatCountsAsPrinted() {
        assertEquals(PrintOutcome.COMPLETED, IosIndexPrinter.outcomeOf(completed = true))
        assertEquals(PrintOutcome.NOT_PRINTED, IosIndexPrinter.outcomeOf(completed = false))
    }

    @Test
    fun anEmptyBankHasNoPagesAndNoPdfToPrint() {
        assertEquals(emptyList(), IosIndexPdf.layout(IndexFixtures.document(emptyList())))
        assertEquals(null, IosIndexPdf.render(IndexFixtures.document(emptyList())))
    }

    @Test
    fun theLargestIndexIsLaidOutAndRenderedWithinTheBudget() {
        // The Bank holds at most Part2Limits.ENTRIES_MAX (2000) casualties; the rows come in a few hundred at most in real use, 2000 is the ceiling.
        val doc = IndexFixtures.document(List(10) { IndexFixtures.year("${2026 - it}", 200, it * 200) })
        var best = Long.MAX_VALUE
        var pages = 0
        repeat(3) {
            val start = TimeSource.Monotonic.markNow()
            pages = pdfPages(IosIndexPdf.render(doc)!!)
            best = minOf(best, start.elapsedNow().inWholeMilliseconds)
        }

        println("IosIndexPdf 2000 rows: $best ms, $pages pages")
        assertTrue(best < BUDGET_MS, "$best ms for the largest index, budget $BUDGET_MS")
    }

    private companion object {
        const val BUDGET_MS = 3000L
    }
}
