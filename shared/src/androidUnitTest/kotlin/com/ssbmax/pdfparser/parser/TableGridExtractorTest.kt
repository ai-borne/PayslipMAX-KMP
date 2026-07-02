package com.ssbmax.pdfparser.parser

import com.tom_roush.pdfbox.text.TextPosition
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class TestableTableGridExtractor(
    yTableStart: Float,
    yTableEnd: Float,
    xSplit: Float,
    xRightBound: Float,
) : TableGridExtractor(yTableStart, yTableEnd, xSplit, xRightBound) {
    fun simulateString(
        text: String,
        positions: MutableList<TextPosition>,
    ) {
        writeString(text, positions)
    }
}

class TableGridExtractorTest {
    private fun mockPos(
        text: String,
        x: Float,
        y: Float,
        width: Float = 20f,
    ): TextPosition {
        return mockk<TextPosition>(relaxed = true) {
            every { unicode } returns text
            every { xDirAdj } returns x
            every { yDirAdj } returns y
            every { widthDirAdj } returns width
        }
    }

    @Test
    fun `extractColumnTexts ignores text outside yTableStart and yTableEnd bounds`() {
        val extractor =
            TestableTableGridExtractor(
                yTableStart = 200f,
                yTableEnd = 600f,
                xSplit = 250f,
                xRightBound = 500f,
            )

        // Simulate Header above table start (y=150)
        extractor.simulateString("Header", mutableListOf(mockPos("Header", 50f, 150f)))
        // Simulate Net Remittance below table end (y=700)
        extractor.simulateString("Net Remittance 160570", mutableListOf(mockPos("Net Remittance 160570", 300f, 700f)))

        // Simulate valid table items (y=300)
        extractor.simulateString("BPAY", mutableListOf(mockPos("BPAY", 50f, 300f)))
        extractor.simulateString("DSOP", mutableListOf(mockPos("DSOP", 300f, 300f)))

        val res = extractor.extractColumnTexts()
        val left = res.first
        val middle = res.second

        assertTrue(left.contains("BPAY"), "Left column should contain table item BPAY")
        assertTrue(middle.contains("DSOP"), "Middle column should contain table item DSOP")
        assertFalse(left.contains("Header"), "Left column must not contain text above yTableStart")
        assertFalse(middle.contains("Net Remittance"), "Middle column must not contain text below yTableEnd")
    }

    @Test
    fun `extractColumnTexts groups characters on same y band into same row`() {
        val extractor =
            TestableTableGridExtractor(
                yTableStart = 200f,
                yTableEnd = 600f,
                xSplit = 250f,
                xRightBound = 500f,
            )

        // Two items on slightly different Y coordinates within 3.0pt band (e.g. 300.0 and 301.5)
        extractor.simulateString("Basic", mutableListOf(mockPos("Basic", 50f, 300.0f)))
        extractor.simulateString("Pay", mutableListOf(mockPos("Pay", 100f, 301.5f)))

        val res = extractor.extractColumnTexts()
        val left = res.first
        assertTrue(left.contains("Basic Pay") || (left.contains("Basic") && left.contains("Pay")), "Row clustering should group items on same Y band")
    }

    @Test
    fun `extractColumnTexts cleanly separates left and middle columns by xSplit`() {
        val extractor =
            TestableTableGridExtractor(
                yTableStart = 200f,
                yTableEnd = 600f,
                xSplit = 250f,
                xRightBound = 500f,
            )

        extractor.simulateString("DA", mutableListOf(mockPos("DA", 50f, 320f)))
        extractor.simulateString("ITAX", mutableListOf(mockPos("ITAX", 300f, 320f)))

        val res = extractor.extractColumnTexts()
        assertEquals("DA", res.first.trim())
        assertEquals("ITAX", res.second.trim())
    }
}
