package com.payslipmax.pdfparser.part2.print

import android.graphics.Canvas
import android.graphics.Paint
import com.payslipmax.pdfparser.utils.SlipLayoutSpec
import com.payslipmax.pdfparser.utils.SlipLayoutSpec.Font
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the clerk compares the paper with the preview field by field, so every line the layout placed must be drawn: where it was
 * placed, in its own type, size and ink, and every rule in the rule colour. The PDF itself is built in memory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SlipPdfTest {
    private class Drawn(val text: String, val x: Float, val y: Float, val paint: Paint)

    private val page = SlipPrintFixtures.pages.first()

    private fun draw(): Pair<List<Drawn>, List<FloatArray>> {
        val texts = mutableListOf<Drawn>()
        val lines = mutableListOf<FloatArray>()
        val canvas = mockk<Canvas>(relaxed = true)
        every { canvas.drawText(any<String>(), any(), any(), any()) } answers { texts += Drawn(firstArg(), secondArg(), thirdArg(), arg(3)) }
        every { canvas.drawLine(any(), any(), any(), any(), any()) } answers { lines += floatArrayOf(firstArg(), secondArg(), thirdArg(), arg(3), arg<Paint>(4).color.toFloat(), arg<Paint>(4).strokeWidth) }
        SlipPdf.draw(canvas, page)
        return texts to lines
    }

    @Test
    fun everyLineOfThePageIsDrawnAtItsPlaceInItsOwnTypeAndInk() {
        val (drawn, _) = draw()

        assertEquals(page.texts.map { it.text }, drawn.map { it.text })
        page.texts.zip(drawn).forEach { (placed, d) ->
            assertEquals(placed.x.toFloat(), d.x)
            assertEquals(placed.font.size.toFloat(), d.paint.textSize)
            assertEquals(placed.font.bold, d.paint.isFakeBoldText)
            assertEquals(placed.font.ink.toInt(), d.paint.color)
            assertEquals(placed.top - d.paint.ascent(), d.y, 0.001f, "the line box starts at its top: the baseline is below it by the ascent")
        }
    }

    @Test
    fun everyRuleIsAHairlineInTheRuleColourAcrossTheContentWidth() {
        val (_, lines) = draw()

        assertEquals(page.rules.size, lines.size)
        assertTrue(page.rules.isNotEmpty())
        page.rules.zip(lines).forEach { (rule, line) ->
            assertEquals(listOf(rule.x1.toFloat(), rule.y.toFloat(), rule.x2.toFloat(), rule.y.toFloat()), line.take(4))
            assertEquals(SlipLayoutSpec.RULE.toInt().toFloat(), line[4])
            assertEquals(SlipLayoutSpec.RULE_WIDTH.toFloat(), line[5], "a hairline")
        }
    }

    @Test
    fun theTypeStylesAreTheOnesTheSpecNames() {
        val fonts = draw().first.map { it.paint.textSize to it.paint.isFakeBoldText }.toSet()

        assertTrue(Font.TITLE.size.toFloat() to Font.TITLE.bold in fonts)
        assertTrue(Font.BODY.size.toFloat() to false in fonts)
    }

    @Test
    fun aWholeSlipIsOnePagePerLaidOutPageInOrderWrittenOnceToTheStreamAndClosed() {
        val pages = SlipPrintFixtures.pages
        val document = RecordingPdfDocument()
        val out = ByteArrayOutputStream()

        SlipPdf.write(pages, pages.indices.toList(), out, document)

        assertEquals((1..pages.size).toList(), document.started)
        assertEquals(pages.size, document.finished)
        assertEquals(RecordingPdfDocument.MARK.decodeToString(), out.toString())
        assertTrue(document.closed)
    }

    @Test
    fun theDocumentIsClosedEvenWhenWritingFails() {
        val document = RecordingPdfDocument(failOnWrite = true)

        runCatching { SlipPdf.write(SlipPrintFixtures.pages, listOf(0), ByteArrayOutputStream(), document) }

        assertTrue(document.closed)
    }
}
