package com.payslipmax.pdfparser.part2.print

/** A multi-page slip with invented text and a deterministic measurer (half the point size per character), for the printer tests. */
internal object SlipPrintFixtures {
    val measurer = SlipTextMeasurer { text, font -> text.length * font.size * 0.5 }

    val document =
        SlipDocument(
            title = "Slip title",
            lastOrder = "Last Part II order: ____",
            notice = "Draft notice",
            orders = listOf(SlipDocumentOrder("Order 1 · Group A", List(40) { n -> SlipDocumentRow("CODE$n", "Narrative $n", List(4) { "Detail $n.$it" }) })),
            pageLabel = "Page {page} of {pages}",
        )

    val pages: List<SlipPage> get() = SlipLayout.paginate(document, measurer)
}

/** Records what is done to a PDF document instead of writing one: Robolectric has no native PDF code, so a real PDF is proven on the device. */
internal class RecordingPdfDocument(private val failOnWrite: Boolean = false) : SlipPdfDocument {
    val started = mutableListOf<Int>()
    val canvases = mutableListOf<android.graphics.Canvas>()
    var finished = 0
    var written = 0
    var closed = false

    override fun startPage(number: Int): android.graphics.Canvas {
        started += number
        return io.mockk.mockk<android.graphics.Canvas>(relaxed = true).also { canvases += it }
    }

    override fun finishPage() {
        finished++
    }

    override fun writeTo(out: java.io.OutputStream) {
        if (failOnWrite) throw java.io.IOException("disk full")
        written++
        out.write(MARK)
    }

    override fun close() {
        closed = true
    }

    companion object {
        val MARK = "%PDF-RECORDED".encodeToByteArray()
    }
}
