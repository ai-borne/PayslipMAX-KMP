package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: [RawLabelNoiseFilter] keeps footer disclaimers and seasonal greetings out of the earnings/deductions lists, where their stray
 * trailing number would show up as phantom pay. Its limits are tight on purpose: too low and a genuine (if unrecognised) PCDA line
 * item is dropped, i.e. real money goes missing silently; too high and prose leaks back in. Each limit is pinned at its edge.
 */
class RawLabelNoiseFilterTest {
    @Test
    fun aLabelOfExactlySixtyCharactersIsKeptAndSixtyOneIsNoise() {
        assertFalse(RawLabelNoiseFilter.isProseNoise("x".repeat(60)))
        assertTrue(RawLabelNoiseFilter.isProseNoise("x".repeat(61)))
    }

    @Test
    fun aLabelOfEightWordsIsKeptAndNineIsNoise() {
        val eight = "aaa bbb ccc ddd eee fff ggg hhh"

        assertFalse(RawLabelNoiseFilter.isProseNoise(eight))
        assertTrue(RawLabelNoiseFilter.isProseNoise("$eight iii"))
    }

    @Test
    fun oneEnglishStopwordIsKeptButTwoMakeItProse() {
        assertFalse(RawLabelNoiseFilter.isProseNoise("arrears of pay"), "a single stopword can sit in a real label")
        assertTrue(RawLabelNoiseFilter.isProseNoise("happy diwali"), "two stopwords is a greeting, however short")
        assertTrue(RawLabelNoiseFilter.isProseNoise("happy new year 2026"))
    }

    @Test
    fun terseRealPcdaLabelsAreNeverNoise() {
        listOf("basic pay", "da", "msp", "dsop subn", "income tax", "hra").forEach {
            assertFalse(RawLabelNoiseFilter.isProseNoise(it), "'$it' is a real line-item label")
        }
    }

    @Test
    fun aLabelMadeOnlyOfAdministrativeFillerIsDatePlaceNoise() {
        assertTrue(RawLabelNoiseFilter.isDatePlaceOnlyNoise("part ii order no dated"))
        assertTrue(RawLabelNoiseFilter.isDatePlaceOnlyNoise("page no"))
    }

    @Test
    fun oneRealWordKeepsALabelOutOfDatePlaceNoise() {
        assertFalse(RawLabelNoiseFilter.isDatePlaceOnlyNoise("order msp"), "a genuine code next to filler is still a real item")
        assertFalse(RawLabelNoiseFilter.isDatePlaceOnlyNoise(""), "an empty label is not filler, it is handled upstream")
    }
}
