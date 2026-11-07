package com.payslipmax.pdfparser.part2.print

/** Records what is done to a PDF document instead of writing one: Robolectric has no native PDF code, so a real PDF is proven on the device. */
internal class RecordingPdfDocument(private val failOnWrite: Boolean = false) : IndexPdfDocument {
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
