package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The edits a form makes to the officer's event: each keeps `Part2Limits` (so a draft can always be saved) and never changes its input. */
class LeaveEventEditorTest {
    private val day = SlotValue.DateValue(CalendarDate(2026, 7, 5))

    private fun two() = LeaveEventEditor.addRequest(LeaveEventEditor.start("AAA"), "BBB")

    @Test
    fun aNewEventHoldsOneRequestForTheChosenCode() {
        val event = LeaveEventEditor.start("GTDAL")
        assertEquals(listOf("GTDAL"), event.requests.map { it.code })
        assertEquals(emptyMap(), event.requests.single().values)
        assertNull(event.lastOrder)
    }

    @Test
    fun aCodeThatIsNotAPlainTokenStartsNothing() {
        assertEquals(emptyList(), LeaveEventEditor.start("A<B").requests)
        assertEquals(emptyList(), LeaveEventEditor.start("").requests)
    }

    @Test
    fun aValueIsSetOnTheChosenRequestOnlyAndNullRemovesIt() {
        val set = LeaveEventEditor.setValue(two(), 1, "leave_from", day)
        assertEquals(emptyMap(), set.requests[0].values)
        assertEquals(mapOf("leave_from" to day), set.requests[1].values)
        assertEquals(emptyMap(), LeaveEventEditor.setValue(set, 1, "leave_from", null).requests[1].values)
    }

    @Test
    fun anIndexOutsideTheEventChangesNothing() {
        val event = two()
        assertEquals(event, LeaveEventEditor.setValue(event, 2, "x", day))
        assertEquals(event, LeaveEventEditor.setValue(event, -1, "x", day))
        assertEquals(event, LeaveEventEditor.setYesNo(event, 5, "ration", true))
        assertEquals(event, LeaveEventEditor.chooseOne(event, 5, "rh:", "A"))
        assertEquals(event, LeaveEventEditor.removeRequest(event, 5))
    }

    @Test
    fun aSlotIdThatIsNotAPlainTokenIsRefused() {
        val event = LeaveEventEditor.start("AAA")
        assertEquals(event, LeaveEventEditor.setValue(event, 0, "a b", day))
        assertEquals(event, LeaveEventEditor.setValue(event, 0, "<x>", day))
    }

    @Test
    fun typedTextIsCutAtTheLimitAndAnOrderNumberAtItsOwn() {
        val long = LeaveEventEditor.setValue(LeaveEventEditor.start("AAA"), 0, "remark", SlotValue.TextValue("x".repeat(Part2Limits.TEXT_MAX + 50)))
        assertEquals(Part2Limits.TEXT_MAX, (long.requests[0].values["remark"] as SlotValue.TextValue).text.length)
        val ref = SlotValue.RefValue(OrderRef("n".repeat(Part2Limits.ORDER_NO_MAX + 9), CalendarDate(2026, 1, 2)))
        val clipped = LeaveEventEditor.setValue(LeaveEventEditor.start("AAA"), 0, "grant", ref)
        assertEquals(Part2Limits.ORDER_NO_MAX, (clipped.requests[0].values["grant"] as SlotValue.RefValue).ref.orderNo.length)
    }

    @Test
    fun aLongerTextLimitKeepsAWholeBreakUpWhileTheDefaultStillCutsAtTwoHundred() {
        // why: a year-by-year list is up to 600 characters (storage and the reader use that limit); cut at 200 it would end in the middle of a line.
        val rows = (0 until 60).joinToString("\n") { "${2000 + it} 30" }
        val whole = LeaveEventEditor.setValue(LeaveEventEditor.start("AAA"), 0, "breakup", SlotValue.TextValue(rows), Part2Limits.BREAKUP_TEXT_MAX)
        assertEquals(rows, (whole.requests[0].values["breakup"] as SlotValue.TextValue).text)
        val cut = LeaveEventEditor.setValue(LeaveEventEditor.start("AAA"), 0, "breakup", SlotValue.TextValue(rows))
        assertEquals(Part2Limits.TEXT_MAX, (cut.requests[0].values["breakup"] as SlotValue.TextValue).text.length)
        val over = LeaveEventEditor.setValue(LeaveEventEditor.start("AAA"), 0, "breakup", SlotValue.TextValue(rows + "x".repeat(Part2Limits.BREAKUP_TEXT_MAX)), Part2Limits.BREAKUP_TEXT_MAX)
        assertEquals(Part2Limits.BREAKUP_TEXT_MAX, (over.requests[0].values["breakup"] as SlotValue.TextValue).text.length, "never past the limit it was given")
    }

    @Test
    fun aNewSlotPastTheValueLimitIsRefusedButAnExistingOneCanStillChange() {
        var event = LeaveEventEditor.start("AAA")
        repeat(Part2Limits.VALUES_PER_REQUEST_MAX) { event = LeaveEventEditor.setValue(event, 0, "s$it", day) }
        assertEquals(Part2Limits.VALUES_PER_REQUEST_MAX, event.requests[0].values.size)
        assertEquals(event, LeaveEventEditor.setValue(event, 0, "one_more", day))
        val changed = LeaveEventEditor.setValue(event, 0, "s0", SlotValue.NumberValue(7))
        assertEquals(SlotValue.NumberValue(7), changed.requests[0].values["s0"])
        assertEquals(event, LeaveEventEditor.setValue(event, 0, "s0", day), "an unchanged value is no change")
    }

    @Test
    fun yesAddsTheAnswerAndNoRemovesItWithoutTouchingOthers() {
        var event = LeaveEventEditor.setYesNo(LeaveEventEditor.start("AAA"), 0, "ration", true)
        event = LeaveEventEditor.setYesNo(event, 0, "other", true)
        assertEquals(setOf("ration", "other"), event.requests[0].answers)
        assertEquals(setOf("other"), LeaveEventEditor.setYesNo(event, 0, "ration", false).requests[0].answers)
        assertEquals(event, LeaveEventEditor.setYesNo(event, 0, "ration", true), "ticking twice is one answer")
    }

    @Test
    fun anAnswerPastTheLimitIsRefused() {
        var event = LeaveEventEditor.start("AAA")
        repeat(Part2Limits.ANSWERS_PER_REQUEST_MAX) { event = LeaveEventEditor.setYesNo(event, 0, "a$it", true) }
        assertEquals(event, LeaveEventEditor.setYesNo(event, 0, "one_more", true))
        assertEquals(event, LeaveEventEditor.setYesNo(event, 0, "x".repeat(Part2Limits.ANSWER_MAX + 1), true))
    }

    @Test
    fun choosingOneReplacesTheEarlierChoiceOfThatQuestionOnly() {
        var event = LeaveEventEditor.setYesNo(LeaveEventEditor.start("AAA"), 0, "ration", true)
        event = LeaveEventEditor.chooseOne(event, 0, "rh_code:", "R12")
        event = LeaveEventEditor.chooseOne(event, 0, "rh_code:", "R21")
        assertEquals(setOf("ration", "rh_code:R21"), event.requests[0].answers)
        assertEquals(setOf("ration"), LeaveEventEditor.chooseOne(event, 0, "rh_code:", null).requests[0].answers)
    }

    @Test
    fun requestsAreAddedUpToTheLimitAndRemovedButNeverTheLast() {
        var event = LeaveEventEditor.start("AAA")
        repeat(Part2Limits.REQUESTS_MAX - 1) { event = LeaveEventEditor.addRequest(event, "C$it") }
        assertEquals(Part2Limits.REQUESTS_MAX, event.requests.size)
        assertEquals(event, LeaveEventEditor.addRequest(event, "ONE_MORE"))
        assertEquals(event, LeaveEventEditor.addRequest(event, "bad code"))
        val smaller = LeaveEventEditor.removeRequest(event, 0)
        assertEquals("C0", smaller.requests.first().code)
        val single = LeaveEventEditor.start("AAA")
        assertEquals(single, LeaveEventEditor.removeRequest(single, 0), "an event keeps at least one request")
    }

    @Test
    fun anUnsetDateIsNeverARealDayAndTheLastOrderIsKept() {
        assertTrue(!LeaveEventEditor.isSet(LeaveEventEditor.UNSET_DATE) && LeaveEventEditor.isSet(CalendarDate(2026, 1, 2)))
        val withOrder = LeaveEventInput(listOf(OccurrenceRequest("AAA")), OrderRef("N/1", CalendarDate(2026, 1, 2)))
        assertEquals(withOrder.lastOrder, LeaveEventEditor.setValue(withOrder, 0, "x", day).lastOrder)
        assertEquals(withOrder.lastOrder, LeaveEventEditor.addRequest(withOrder, "BBB").lastOrder)
    }

    @Test
    fun anAnswerLongerThanTheLimitIsRefusedEvenWithRoomForIt() {
        // why: the answers cap alone would refuse it only once the request is full; the length is its own limit.
        val event = LeaveEventEditor.start("AAA")
        assertEquals(event, LeaveEventEditor.setYesNo(event, 0, "x".repeat(Part2Limits.ANSWER_MAX + 1), true))
        assertEquals(setOf("x".repeat(Part2Limits.ANSWER_MAX)), LeaveEventEditor.setYesNo(event, 0, "x".repeat(Part2Limits.ANSWER_MAX), true).requests[0].answers)
    }

    @Test
    fun aChoiceIsRefusedWhenItWouldPassTheAnswerLimitOrBeTooLongAndAcceptedJustBelow() {
        var below = LeaveEventEditor.start("AAA")
        repeat(Part2Limits.ANSWERS_PER_REQUEST_MAX - 1) { below = LeaveEventEditor.setYesNo(below, 0, "a$it", true) }
        assertEquals(Part2Limits.ANSWERS_PER_REQUEST_MAX, LeaveEventEditor.chooseOne(below, 0, "p:", "x").requests[0].answers.size, "the last free place is taken")
        val full = LeaveEventEditor.setYesNo(below, 0, "last", true)
        assertEquals(full, LeaveEventEditor.chooseOne(full, 0, "p:", "x"), "no place left")
        assertEquals(below, LeaveEventEditor.chooseOne(below, 0, "p:", "x".repeat(Part2Limits.ANSWER_MAX)), "prefix and option together are over the length")
    }

    @Test
    fun aRequestForACodeThatIsNotAPlainTokenIsRefusedWithRoomToSpare() {
        val event = LeaveEventEditor.start("AAA")
        assertEquals(event, LeaveEventEditor.addRequest(event, "bad code"))
        assertEquals(event, LeaveEventEditor.addRequest(event, ""))
        assertEquals(event, LeaveEventEditor.addRequest(event, "<x>"))
    }
}
