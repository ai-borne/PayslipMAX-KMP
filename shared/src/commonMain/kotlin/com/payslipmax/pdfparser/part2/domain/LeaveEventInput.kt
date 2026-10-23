package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate

/**
 * A published Part II order the officer quotes: its number and date, as typed from the order itself. Not an identity number: it names
 * an order, never the officer.
 */
data class OrderRef(
    val orderNo: String,
    val date: CalendarDate,
)

/** What the officer answered for one slot of a template. */
sealed interface SlotValue {
    data class DateValue(val date: CalendarDate) : SlotValue

    data class NumberValue(val number: Int) : SlotValue

    data class TextValue(val text: String) : SlotValue

    data class RefValue(val ref: OrderRef) : SlotValue
}

/**
 * One occurrence the officer asks for: the manual's [code], the [values] for the template's slots (by slot id; one answer serves every
 * linked template that declares the same id), and the yes/no [answers] the rule conditions read (for example whether they are on
 * ration strength). Two years granted together are two requests of the same code.
 */
data class OccurrenceRequest(
    val code: String,
    val values: Map<String, SlotValue> = emptyMap(),
    val answers: Set<String> = emptySet(),
)

/** An event, such as leave: the requests that make it and the officer's last published Part II order ([lastOrder], blank when none). */
data class LeaveEventInput(
    val requests: List<OccurrenceRequest>,
    val lastOrder: OrderRef? = null,
)
