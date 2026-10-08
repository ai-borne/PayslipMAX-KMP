package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuidePinsTest {
    @Test
    fun theNewestPinComesFirst() {
        val pins = GuidePins.Empty.toggle("RB-A").toggle("RB-B").toggle("RB-C")

        assertEquals(listOf("RB-C", "RB-B", "RB-A"), pins.newestFirst)
    }

    @Test
    fun togglingAPinnedCardUnpinsItAndKeepsTheOthersInOrder() {
        val pins = GuidePins.Empty.toggle("RB-A").toggle("RB-B").toggle("RB-C").toggle("RB-B")

        assertFalse(pins.isPinned("RB-B"))
        assertEquals(listOf("RB-C", "RB-A"), pins.newestFirst)
    }

    @Test
    fun pinningAgainAfterUnpinningPutsItAtTheFront() {
        val pins = GuidePins.Empty.toggle("RB-A").toggle("RB-B").toggle("RB-A").toggle("RB-A")

        assertEquals(listOf("RB-A", "RB-B"), pins.newestFirst)
    }

    @Test
    fun idsTheBundleNoLongerHoldsAreDroppedAndTheRestKeepTheirOrder() {
        val pins = GuidePins.Empty.toggle("RB-A").toggle("RB-GONE").toggle("RB-C")

        val kept = pins.retainKnown { it != "RB-GONE" }

        assertEquals(listOf("RB-C", "RB-A"), kept.newestFirst)
    }

    @Test
    fun theStoredFormRoundTripsInOrder() {
        val pins = GuidePins.Empty.toggle("RB-A").toggle("RB-B")

        assertEquals(pins, GuidePins.fromStored(pins.toStored()))
    }

    @Test
    fun aDamagedStoredValueKeepsOnlyPlainCardIdsOnce() {
        val raw = listOf("RB-SS-T068", "", "<script>", "RB-SS-T068", "has space", "x".repeat(65), "RB-C13.05_a", "é").joinToString("\n")

        assertEquals(listOf("RB-C13.05_a", "RB-SS-T068"), GuidePins.fromStored(raw).newestFirst)
    }

    @Test
    fun nothingStoredMeansNoPins() {
        assertTrue(GuidePins.fromStored(null).newestFirst.isEmpty())
        assertTrue(GuidePins.fromStored("").newestFirst.isEmpty())
    }

    @Test
    fun anIdThatIsNotPlainCanNeverBePinned() {
        assertEquals(GuidePins.Empty, GuidePins.Empty.toggle("a\nb"))
    }
}
