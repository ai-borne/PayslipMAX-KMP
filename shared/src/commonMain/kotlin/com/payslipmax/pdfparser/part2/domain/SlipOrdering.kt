package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * Arranges drafted occurrences into orders. Para 6.4: all the occurrences of one event go in one order, in date order. Para 7.1: a
 * Group B claim is never combined with A, AM, C or CM, and (owner decision, from practice) each claim is an order of its own. The order
 * is [OrderFamily.FINANCIAL] when any occurrence in it is A or AM; a serial with no printed group joins the order but never decides its
 * family (no group is inferred). The order header lists each group letter its occurrences carry, once, in the manual's order.
 */
object SlipOrdering {
    private const val CLAIM_GROUP = "B"
    private val financial = setOf("A", "AM")
    private val letters = listOf("A", "AM", "B", "C", "CM")
    private val nonFinancial = setOf("C", "CM")

    fun arrange(occurrences: List<Part2Occurrence>): List<Part2Order> {
        val distinct = occurrences.distinctBy { listOf(it.code, it.narrative, it.from, it.to, it.data, it.authority, it.certificate) }
        val (claims, rest) = distinct.partition { it.group == CLAIM_GROUP }
        val main = rest.takeIf { it.isNotEmpty() }?.let { Part2Order(familyOf(it), inDateOrder(it), groupsOf(it)) }
        return listOfNotNull(main) + inDateOrder(claims).map { Part2Order(OrderFamily.CLAIM, listOf(it), groupsOf(listOf(it))) }
    }

    private fun groupsOf(occurrences: List<Part2Occurrence>): List<String> {
        val present = occurrences.mapNotNull { it.group }.toSet()
        return letters.filter { it in present }
    }

    /** Stable: occurrences on one day keep the order they were drafted in, so a requested code stays ahead of its links. */
    private fun inDateOrder(occurrences: List<Part2Occurrence>): List<Part2Occurrence> =
        occurrences.sortedBy { occurrence -> (occurrence.from ?: occurrence.to)?.let { CivilCalendar.dayNumber(it) } ?: Long.MAX_VALUE }

    private fun familyOf(occurrences: List<Part2Occurrence>): OrderFamily {
        val groups = occurrences.mapNotNull { it.group }.toSet()
        return when {
            groups.any { it in financial } -> OrderFamily.FINANCIAL
            groups.any { it in nonFinancial } -> OrderFamily.NON_FINANCIAL
            else -> OrderFamily.UNGROUPED
        }
    }
}
