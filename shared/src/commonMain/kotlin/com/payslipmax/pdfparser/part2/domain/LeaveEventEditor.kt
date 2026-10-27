package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate

/**
 * Pure edits to the officer's event, each returning a new one and refusing (returning it unchanged) whatever would break `Part2Limits`,
 * so a form can always save what it holds. An edit never reads a template: whether an answer makes sense is the engine's call.
 */
object LeaveEventEditor {
    /** A date not chosen yet; it never exists on a calendar, so the engine names it as a bad date and a half-filled order reference is caught. */
    val UNSET_DATE = CalendarDate(0, 0, 0)

    fun isSet(date: CalendarDate): Boolean = date != UNSET_DATE

    /** A new event with one request for [code]; a code that is not a plain token starts nothing. */
    fun start(code: String): LeaveEventInput = if (isCode(code)) LeaveEventInput(listOf(OccurrenceRequest(code))) else LeaveEventInput(emptyList())

    /**
     * Sets (or, with null, removes) the answer to [slot] on request [index]. Text is cut at [textMax] (the ordinary text limit unless the
     * slot is a year-by-year list, which keeps `Part2Limits.BREAKUP_TEXT_MAX`), an order number at its own.
     */
    fun setValue(
        event: LeaveEventInput,
        index: Int,
        slot: String,
        value: SlotValue?,
        textMax: Int = Part2Limits.TEXT_MAX,
    ): LeaveEventInput {
        if (!Part2Limits.isPlainToken(slot, Part2Limits.SLOT_ID_MAX)) return event
        return edit(event, index) { request ->
            val bounded = value?.let { bounded(it, textMax) }
            when {
                bounded == null -> request.copy(values = request.values - slot)
                slot !in request.values && request.values.size >= Part2Limits.VALUES_PER_REQUEST_MAX -> request
                else -> request.copy(values = request.values + (slot to bounded))
            }
        }
    }

    /** Holds [answer] on request [index] when [yes], drops it otherwise. */
    fun setYesNo(
        event: LeaveEventInput,
        index: Int,
        answer: String,
        yes: Boolean,
    ): LeaveEventInput =
        edit(event, index) { request ->
            when {
                !yes -> request.copy(answers = request.answers - answer)
                answer.length > Part2Limits.ANSWER_MAX || answer in request.answers -> request
                request.answers.size >= Part2Limits.ANSWERS_PER_REQUEST_MAX -> request
                else -> request.copy(answers = request.answers + answer)
            }
        }

    /** Holds [prefix] + [option] on request [index] in place of any earlier choice under [prefix]; a null [option] clears the choice. */
    fun chooseOne(
        event: LeaveEventInput,
        index: Int,
        prefix: String,
        option: String?,
    ): LeaveEventInput =
        edit(event, index) { request ->
            val kept = request.answers.filterNot { it.startsWith(prefix) }.toSet()
            val chosen = option?.let { prefix + it }
            when {
                chosen == null -> request.copy(answers = kept)
                chosen.length > Part2Limits.ANSWER_MAX || kept.size >= Part2Limits.ANSWERS_PER_REQUEST_MAX -> request
                else -> request.copy(answers = kept + chosen)
            }
        }

    /** Adds a request for [code] at the end, up to `Part2Limits.REQUESTS_MAX`. */
    fun addRequest(
        event: LeaveEventInput,
        code: String,
    ): LeaveEventInput =
        if (!isCode(code) || event.requests.size >= Part2Limits.REQUESTS_MAX) event else event.copy(requests = event.requests + OccurrenceRequest(code))

    /** Removes request [index]; the last one stays, since an event with no request is nothing. */
    fun removeRequest(
        event: LeaveEventInput,
        index: Int,
    ): LeaveEventInput = if (index !in event.requests.indices || event.requests.size == 1) event else event.copy(requests = event.requests.filterIndexed { i, _ -> i != index })

    private fun isCode(code: String) = Part2Limits.isPlainToken(code, Part2Limits.CODE_MAX)

    private fun edit(
        event: LeaveEventInput,
        index: Int,
        change: (OccurrenceRequest) -> OccurrenceRequest,
    ): LeaveEventInput {
        val request = event.requests.getOrNull(index) ?: return event
        return event.copy(requests = event.requests.mapIndexed { i, r -> if (i == index) change(request) else r })
    }

    private fun bounded(
        value: SlotValue,
        textMax: Int,
    ): SlotValue =
        when (value) {
            is SlotValue.TextValue -> SlotValue.TextValue(value.text.take(textMax))
            is SlotValue.RefValue -> SlotValue.RefValue(value.ref.copy(orderNo = value.ref.orderNo.take(Part2Limits.ORDER_NO_MAX)))
            is SlotValue.DateValue, is SlotValue.NumberValue -> value
        }
}
