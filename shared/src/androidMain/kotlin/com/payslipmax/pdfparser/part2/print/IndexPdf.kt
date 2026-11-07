package com.payslipmax.pdfparser.part2.print

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.payslipmax.pdfparser.utils.IndexLayoutSpec
import com.payslipmax.pdfparser.utils.IndexLayoutSpec.Font
import com.payslipmax.pdfparser.utils.PdfLayoutSpec
import java.io.OutputStream

/** The paint of each type style: the same sizes and inks the spec names, which iOS draws with its own fonts. Made once and shared by the measurer and the drawing. */
internal object IndexPaints {
    private val byFont =
        Font.entries.associateWith { font ->
            Paint().apply {
                textSize = font.size.toFloat()
                isFakeBoldText = font.bold
                isAntiAlias = true
                color = font.ink.toInt()
            }
        }

    operator fun get(font: Font): Paint = byFont.getValue(font)
}

/** Measures text with the paints the page is drawn with, so a line breaks where it will be drawn. */
internal class PaintMeasurer : IndexTextMeasurer {
    override fun width(
        text: String,
        font: Font,
    ): Double = IndexPaints[font].measureText(text).toDouble()
}

/** The part of Android's `PdfDocument` the index uses, so the writer can be tested without the framework's native PDF code (which Robolectric does not have). */
internal interface IndexPdfDocument : AutoCloseable {
    fun startPage(number: Int): Canvas

    fun finishPage()

    fun writeTo(out: OutputStream)
}

internal class FrameworkPdfDocument : IndexPdfDocument {
    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null

    override fun startPage(number: Int): Canvas {
        val started = document.startPage(PdfDocument.PageInfo.Builder(PdfLayoutSpec.PAGE_WIDTH, PdfLayoutSpec.PAGE_HEIGHT, number).create())
        page = started
        return started.canvas
    }

    override fun finishPage() {
        page?.let(document::finishPage)
        page = null
    }

    override fun writeTo(out: OutputStream) = document.writeTo(out)

    override fun close() = document.close()
}

/** Draws laid-out index pages into a PDF in memory. The same page model is drawn on iOS by `IosIndexPdf`. */
internal object IndexPdf {
    private val rulePaint =
        Paint().apply {
            color = IndexLayoutSpec.RULE.toInt()
            strokeWidth = IndexLayoutSpec.RULE_WIDTH.toFloat()
        }

    /** Writes the pages at [indices] (zero based, in order) to [out] as one PDF built in [document], which is closed whatever happens. */
    fun write(
        pages: List<IndexPage>,
        indices: List<Int>,
        out: OutputStream,
        document: IndexPdfDocument = FrameworkPdfDocument(),
    ) {
        document.use {
            indices.forEachIndexed { n, index ->
                draw(it.startPage(n + 1), pages[index])
                it.finishPage()
            }
            it.writeTo(out)
        }
    }

    /** Draws one laid-out [page] on [canvas]. A line's box starts at its top, so its baseline is the ascent below it. */
    fun draw(
        canvas: Canvas,
        page: IndexPage,
    ) {
        for (text in page.texts) {
            val paint = IndexPaints[text.font]
            canvas.drawText(text.text, text.x.toFloat(), text.top - paint.ascent(), paint)
        }
        for (rule in page.rules) canvas.drawLine(rule.x1.toFloat(), rule.y.toFloat(), rule.x2.toFloat(), rule.y.toFloat(), rulePaint)
    }
}
