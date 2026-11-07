package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: these helpers replaced lookaround regexes that were quadratic on Kotlin/Native (a 4-11 minute iOS-only parse stall). They must
 * keep the old boundary semantics exactly (a match is not flanked by an ASCII letter or digit) or every totals/keyword lookup that
 * uses them changes behaviour on one platform only.
 */
class WholeWordScannerTest {
    @Test
    fun aWordInsideALongerWordIsNotAMatch() {
        assertNull(findWholeWordIgnoreCase("SUBTOTAL DAY DA1", "da"))
        assertEquals(0..1, findWholeWordIgnoreCase("DA 12", "da"))
    }

    @Test
    fun theSearchIsCaseInsensitiveAndReturnsTheRangeInTheOriginalText() {
        assertEquals(6..7, findWholeWordIgnoreCase("Basic Da paid", "DA"))
    }

    @Test
    fun punctuationAndNonAsciiLettersAreBoundaries() {
        assertEquals(0..1, findWholeWordIgnoreCase("DA-ARR", "da"))
        assertEquals(1..2, findWholeWordIgnoreCase("éDA", "da"), "only ASCII letters and digits block a match, as the old regex did")
    }

    @Test
    fun aRejectedOccurrenceDoesNotStopTheScanForALaterOne() {
        assertEquals(8..9, findWholeWordIgnoreCase("DAY DA1 DA", "da"))
    }

    @Test
    fun theSearchStartsAtTheGivenIndex() {
        assertEquals(6..7, findWholeWordIgnoreCase("DA 12 DA", "da", 1))
        assertNull(findWholeWordIgnoreCase("DA 12", "da", 1))
    }

    @Test
    fun anEmptyWordNeverMatches() {
        assertNull(findWholeWordIgnoreCase("anything", ""))
    }

    @Test
    fun replaceTouchesOnlyWholeWordOccurrences() {
        assertEquals("X and DAY and X", replaceWholeWordIgnoreCase("DA and DAY and da", "DA", "X"))
        assertEquals("unchanged", replaceWholeWordIgnoreCase("unchanged", "DA", "X"))
    }

    @Test
    fun aKeyedNumberMustFollowTheKeyDirectly() {
        assertEquals(1234.0, findKeyedNumber("Gross: 1234", "gross"))
        assertEquals(500.0, findKeyedNumber("Total Rs. 500", "Total"))
        assertEquals(75.0, findKeyedNumber("Total - 75", "Total"))
    }

    @Test
    fun aKeyNotFollowedByANumberFallsThroughToALaterOccurrence() {
        assertEquals(500.0, findKeyedNumber("Total of items. Total: 500", "Total"))
    }

    @Test
    fun aNumberFarFromTheKeyOrAKeyInsideAnotherWordYieldsNothing() {
        assertNull(findKeyedNumber("Total of items 500", "Total"), "words between the key and the number break adjacency")
        assertNull(findKeyedNumber("SubTotal 500", "Total"))
        assertNull(findKeyedNumber("no key here 5", "Total"))
    }
}
