package com.payslipmax.pdfparser.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfLetterFormatterTest {
    // Fake metric: 1 unit of width per character — makes wrap boundaries deterministic in tests.
    private val charWidth: (String) -> Double = { it.length.toDouble() }

    @Test
    fun fileNameIsFilesystemSafeAndStable() {
        assertEquals("PCDA_Representation_02_2026.pdf", PdfLetterFormatter.fileName("02/2026"))
        // No path separators or slashes survive.
        val name = PdfLetterFormatter.fileName("1/2 2026")
        assertTrue(name.none { it == '/' || it == ' ' })
        assertTrue(name.endsWith(".pdf"))
    }

    @Test
    fun contentLinesLeadWithTitleThenSpacerThenBody() {
        val lines = PdfLetterFormatter.contentLines("Subject X", "line1\nline2")
        assertEquals(PdfLine("Subject X", isTitle = true), lines[0])
        assertEquals(PdfLine("", isTitle = false), lines[1])
        assertEquals(listOf("line1", "line2"), lines.drop(2).map { it.text })
        assertTrue(lines.drop(1).none { it.isTitle }, "only the first line is the title")
    }

    @Test
    fun wrapLineBreaksGreedilyAtWidth() {
        // maxWidth 10: "aaa bbb" (7) fits; adding " ccc" (11) overflows -> second line.
        val wrapped = PdfLetterFormatter.wrapLine("aaa bbb ccc", maxWidth = 10.0, measure = charWidth)
        assertEquals(listOf("aaa bbb", "ccc"), wrapped)
    }

    @Test
    fun wrapLinePreservesBlankLinesAsParagraphSpacing() {
        assertEquals(listOf(""), PdfLetterFormatter.wrapLine("   ", maxWidth = 100.0, measure = charWidth))
    }

    @Test
    fun wrapLineEmitsAnOverlongWordOnItsOwnLine() {
        val wrapped = PdfLetterFormatter.wrapLine("short superlongwordthatexceeds", maxWidth = 8.0, measure = charWidth)
        assertEquals(listOf("short", "superlongwordthatexceeds"), wrapped)
    }

    @Test
    fun breakingLongWordsFitsEveryLineAndKeepsEveryCharacter() {
        val wrapped = PdfLetterFormatter.wrapLine("ab " + "x".repeat(25), maxWidth = 10.0, breakLongWords = true, measure = charWidth)

        assertEquals(listOf("ab", "xxxxxxxxxx", "xxxxxxxxxx", "xxxxx"), wrapped)
        assertTrue(wrapped.all { it.length <= 10 })
    }

    @Test
    fun breakingLongWordsNeverSplitsASurrogatePair() {
        val pair = "\uD83D\uDE00"
        val wrapped = PdfLetterFormatter.wrapLine(pair.repeat(6), maxWidth = 5.0, breakLongWords = true, measure = charWidth)

        assertEquals(pair.repeat(6), wrapped.joinToString(""))
        assertTrue(wrapped.all { line -> line.none { it.isHighSurrogate() } || line.length % 2 == 0 }, "a pair stays whole: $wrapped")
        assertTrue(wrapped.all { !it.last().isHighSurrogate() })
    }

    @Test
    fun breakingLongWordsLeavesWordsThatFitAlone() {
        assertEquals(listOf("aaa bbb", "ccc"), PdfLetterFormatter.wrapLine("aaa bbb ccc", maxWidth = 10.0, breakLongWords = true, measure = charWidth))
    }

    @Test
    fun aPairThatCannotFitEvenAloneIsSplitRatherThanLoopingForever() {
        val pair = "\uD83D\uDE00"

        assertEquals(pair, PdfLetterFormatter.wrapLine(pair, maxWidth = 1.0, breakLongWords = true, measure = charWidth).joinToString(""))
    }

    @Test
    fun aWordExactlyAsWideAsTheLineIsNotBroken() {
        assertEquals(listOf("x".repeat(10)), PdfLetterFormatter.wrapLine("x".repeat(10), maxWidth = 10.0, breakLongWords = true, measure = charWidth))
        assertEquals(listOf("ab", "x".repeat(10)), PdfLetterFormatter.wrapLine("ab " + "x".repeat(10), maxWidth = 10.0, breakLongWords = true, measure = charWidth), "it moves to its own line whole")
    }

    @Test
    fun aSingleCharacterWiderThanTheLineIsStillPrintedOnce() {
        assertEquals(listOf("a", "b"), PdfLetterFormatter.wrapLine("a b", maxWidth = 0.5, breakLongWords = true, measure = charWidth))
    }
}
