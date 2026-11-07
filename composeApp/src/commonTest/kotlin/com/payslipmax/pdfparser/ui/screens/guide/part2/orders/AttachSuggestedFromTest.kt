package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: casualties on one order usually carry the same date, so the officer adding the next one should not start the picker from today
 * again. The date offered is the From of the casualty he added or ticked most recently on THIS order; none ticked, nothing offered.
 */
class AttachSuggestedFromTest {
    private val march = SyntheticBank.entry("a", from = CalendarDate(2026, 3, 1)).copy(createdAt = 10L)
    private val june = SyntheticBank.entry("b", from = CalendarDate(2026, 6, 1)).copy(createdAt = 20L)
    private val april = SyntheticBank.entry("c", from = CalendarDate(2026, 4, 1)).copy(createdAt = 30L)

    @Test
    fun theMostRecentlyCreatedTickedCasualtyLendsItsDate() {
        val state = AttachState(choices = listOf(march, june, april), ticked = setOf("a", "b"))

        assertEquals(CalendarDate(2026, 6, 1), state.suggestedFrom)
    }

    @Test
    fun aWaitingCasualtyThatIsNotTickedLendsNothing() {
        val state = AttachState(choices = listOf(march, june, april), ticked = setOf("a"))

        assertEquals(CalendarDate(2026, 3, 1), state.suggestedFrom, "april is newer but is not on this order")
    }

    @Test
    fun withNothingTickedNothingIsOffered() {
        assertNull(AttachState(choices = listOf(march)).suggestedFrom)
    }
}
