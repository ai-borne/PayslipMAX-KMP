package com.ssbmax.pdfparser.ui.screens

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiInsightsBottomSheetTest {
    @Test
    fun testParseHeader1() {
        val markdown = "# Title Header"
        val result = parseMarkdown(markdown)

        // Assert markdown symbol is removed
        assertEquals("Title Header", result.text)

        // Assert bold style and size (24.sp corresponds to TextSizeHuge)
        val styleRange = result.spanStyles.firstOrNull()
        assertTrue(styleRange != null, "Should have a span style applied")
        assertEquals(FontWeight.Bold, styleRange.item.fontWeight)
        assertEquals(24.sp, styleRange.item.fontSize)
    }

    @Test
    fun testParseHeader2() {
        val markdown = "## Sub Header"
        val result = parseMarkdown(markdown)

        assertEquals("Sub Header", result.text)

        val styleRange = result.spanStyles.firstOrNull()
        assertTrue(styleRange != null, "Should have a span style applied")
        assertEquals(FontWeight.Bold, styleRange.item.fontWeight)
        assertEquals(18.sp, styleRange.item.fontSize) // TextSizeExtraLarge is 18.sp
    }

    @Test
    fun testParseBoldInlineText() {
        val markdown = "This is **bold** text."
        val result = parseMarkdown(markdown)

        // Markdown tags should be stripped
        assertEquals("This is bold text.", result.text)

        // Assert the word "bold" has bold weight applied
        val boldRange = result.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        assertTrue(boldRange != null, "Should have bold styled segment")
        assertEquals(8, boldRange.start)
        assertEquals(12, boldRange.end)
    }

    @Test
    fun testParseListItems() {
        val markdown = "- First item\n* Second **bold** item"
        val result = parseMarkdown(markdown)

        val lines = result.text.split("\n")
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("•  First item"))
        assertTrue(lines[1].startsWith("•  Second bold item"))

        // Check bold formatting inside list item
        val boldRange = result.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        assertTrue(boldRange != null, "Should have bold styled segment in list item")
    }

    @Test
    fun testParseMultiLineAndEmpty() {
        val markdown = "# Title\n\nSome standard paragraph text."
        val result = parseMarkdown(markdown)

        val lines = result.text.split("\n")
        assertEquals(3, lines.size)
        assertEquals("Title", lines[0])
        assertEquals("", lines[1])
        assertEquals("Some standard paragraph text.", lines[2])
    }
}
