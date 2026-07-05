package com.payslipmax.pdfparser.parser

import com.tom_roush.pdfbox.text.TextPosition
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class TestableTokenScanner : TokenScanner() {
    fun simulateLine(positions: MutableList<TextPosition>) {
        writeString(positions.joinToString("") { it.unicode ?: "" }, positions)
    }
}

/**
 * Phase 2: verifies the Android adapter turns PDFBox glyphs into word-level [PositionedToken]s with
 * correct text grouping and union bounding boxes, using mocked [TextPosition]s (no device needed).
 */
class TokenScannerTest {
    private fun glyph(
        ch: String,
        x: Float,
        y: Float,
        width: Float = 6f,
        height: Float = 10f,
    ): TextPosition =
        mockk<TextPosition>(relaxed = true) {
            every { unicode } returns ch
            every { xDirAdj } returns x
            every { yDirAdj } returns y
            every { widthDirAdj } returns width
            every { heightDir } returns height
        }

    @Test
    fun `adjacent glyphs within gap form a single word token`() {
        val scanner = TestableTokenScanner()
        // "DA" — two glyphs, contiguous (no gap > 3f).
        scanner.simulateLine(mutableListOf(glyph("D", 10f, 100f), glyph("A", 16f, 100f)))

        val tokens = scanner.tokens()
        assertEquals(1, tokens.size)
        assertEquals("DA", tokens.first().text)
    }

    @Test
    fun `a horizontal gap splits glyphs into separate tokens`() {
        val scanner = TestableTokenScanner()
        // "DA" then a large x-jump to "9876" — two distinct tokens (label + amount).
        scanner.simulateLine(
            mutableListOf(
                glyph("D", 10f, 100f),
                glyph("A", 16f, 100f),
                glyph("9", 200f, 100f),
                glyph("8", 206f, 100f),
                glyph("7", 212f, 100f),
                glyph("6", 218f, 100f),
            ),
        )

        val tokens = scanner.tokens()
        assertEquals(listOf("DA", "9876"), tokens.map { it.text })
    }

    @Test
    fun `token bounding box is the union of its glyph boxes`() {
        val scanner = TestableTokenScanner()
        scanner.simulateLine(
            mutableListOf(
                glyph("A", 10f, 100f, width = 6f, height = 10f),
                glyph("B", 16f, 100f, width = 6f, height = 10f),
            ),
        )

        val token = scanner.tokens().single()
        assertEquals(10f, token.x)
        assertEquals(100f, token.y)
        assertEquals(12f, token.width) // 22 (right of B) - 10 (left of A)
        assertEquals(10f, token.height)
    }

    @Test
    fun `whitespace glyphs are dropped and do not create empty tokens`() {
        val scanner = TestableTokenScanner()
        scanner.simulateLine(
            mutableListOf(glyph("A", 10f, 100f), glyph(" ", 16f, 100f), glyph("B", 22f, 100f)),
        )

        val tokens = scanner.tokens()
        assertTrue(tokens.none { it.text.isBlank() })
        assertEquals(listOf("A", "B"), tokens.map { it.text })
    }
}
