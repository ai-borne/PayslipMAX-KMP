package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks down the current (pre-Phase-2) behavior of [negateHindiTransliterations] and [parseTotals]
 * before their lookaround-regex implementation is replaced with a manual indexOf-based scan for
 * Kotlin/Native performance (see docs/AI_INSIGHTS_PIPELINE.md). Every assertion here must keep
 * passing unchanged after Phase 2's rewrite.
 */
class ParserUtilsTest {
    // ---- negateHindiTransliterations ----

    @Test
    fun exactWholeWordMatchIsReplaced() {
        val result = negateHindiTransliterations("start kuula end")
        assertEquals("start end", result)
    }

    @Test
    fun wordAsSubstringOfLargerWordIsNotReplaced() {
        // "ka" is a transliteration word, but must not match inside "Kalyan".
        val result = negateHindiTransliterations("Officer Kalyan Singh")
        assertEquals("Officer Kalyan Singh", result)
    }

    @Test
    fun matchIsCaseInsensitive() {
        val result = negateHindiTransliterations("prefix KUULA suffix")
        assertEquals("prefix suffix", result)
    }

    @Test
    fun wordAdjacentToPunctuationIsReplaced() {
        val result = negateHindiTransliterations("value(kuula)tail")
        assertEquals("value( )tail", result)
    }

    @Test
    fun wordAdjacentToDigitIsNotReplaced() {
        // Old regex used (?<![a-zA-Z0-9]) / (?![a-zA-Z0-9]) boundaries, so a word fused to a
        // digit with no separator is not a "whole word" match and must survive untouched.
        val result = negateHindiTransliterations("100kuula200")
        assertEquals("100kuula200", result)
    }

    @Test
    fun matchAtStartOfStringIsReplaced() {
        // No leading/trailing trim happens - only internal whitespace runs are collapsed - so the
        // replacement space plus the original separator space survive as a single leading space.
        val result = negateHindiTransliterations("kuula trailing text")
        assertEquals(" trailing text", result)
    }

    @Test
    fun matchAtEndOfStringIsReplaced() {
        val result = negateHindiTransliterations("leading text kuula")
        assertEquals("leading text ", result)
    }

    @Test
    fun multipleNonOverlappingMatchesAreAllReplaced() {
        val result = negateHindiTransliterations("kuula middle kuula")
        assertEquals(" middle ", result)
    }

    @Test
    fun noMatchPresentLeavesTextUnchanged() {
        val result = negateHindiTransliterations("Officer Name Basic Pay")
        assertEquals("Officer Name Basic Pay", result)
    }

    // ---- parseTotals ----

    @Test
    fun parseTotalsWithColonSeparator() {
        val text = "Gross Pay: 150000 Total Deductions: 30000 Net Remittance: 120000"
        val (gross, deductions, net) = parseTotals(text)
        assertEquals(150000.0, gross)
        assertEquals(30000.0, deductions)
        assertEquals(120000.0, net)
    }

    @Test
    fun parseTotalsWithDashAndEnDashSeparators() {
        val dashText = "Gross Pay - 100000 Total Deductions - 20000 Net Remittance - 80000"
        val (dashGross, dashDeductions, dashNet) = parseTotals(dashText)
        assertEquals(100000.0, dashGross)
        assertEquals(20000.0, dashDeductions)
        assertEquals(80000.0, dashNet)

        val enDashText = "Gross Pay – 100000 Total Deductions – 20000 Net Remittance – 80000"
        val (enDashGross, enDashDeductions, enDashNet) = parseTotals(enDashText)
        assertEquals(100000.0, enDashGross)
        assertEquals(20000.0, enDashDeductions)
        assertEquals(80000.0, enDashNet)
    }

    @Test
    fun parseTotalsWithNoSeparator() {
        val text = "Gross Pay 233016 Total Deductions 93412 Net Remittance 139604"
        val (gross, deductions, net) = parseTotals(text)
        assertEquals(233016.0, gross)
        assertEquals(93412.0, deductions)
        assertEquals(139604.0, net)
    }

    @Test
    fun parseTotalsWithRsPrefix() {
        val text = "Gross Pay: Rs. 150000 Total Deductions: Rs.30000 Net Remittance: Rs 120000"
        val (gross, deductions, net) = parseTotals(text)
        assertEquals(150000.0, gross)
        assertEquals(30000.0, deductions)
        assertEquals(120000.0, net)
    }

    @Test
    fun parseTotalsFallsBackToHindiTransliteratedKeys() {
        val text = "kuula Aaya 200000 kuula kTaOtI 45000 inavala p`oiYat Qana 155000"
        val (gross, deductions, net) = parseTotals(text)
        assertEquals(200000.0, gross)
        assertEquals(45000.0, deductions)
        assertEquals(155000.0, net)
    }

    @Test
    fun parseTotalsWithNoMatchReturnsZeroes() {
        val (gross, deductions, net) = parseTotals("nothing relevant in this text")
        assertEquals(0.0, gross)
        assertEquals(0.0, deductions)
        assertEquals(0.0, net)
    }

    @Test
    fun parseTotalsRemittanceLabelBlockFallback() {
        // PDFKit artifact ordering: labels serialised before amounts.
        val text = "REMITTANCE Total Debit 400000 27119 581007"
        val (_, deductions, net) = parseTotals(text)
        assertEquals(400000.0, net)
        assertEquals(581007.0, deductions)
    }
}
