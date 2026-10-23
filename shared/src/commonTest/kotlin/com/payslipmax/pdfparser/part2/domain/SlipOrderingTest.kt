package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Para 6.4 puts all the occurrences of one event in one order, in date order; para 7.1 keeps a Group B claim out of any order with
 * A, AM, C or CM. A slip that broke either would be rejected by the unit's HRMS, so the arrangement is pinned on its own.
 */
class SlipOrderingTest {
    private fun occurrence(
        code: String,
        group: String?,
        from: CalendarDate?,
        to: CalendarDate? = null,
    ) = Part2Occurrence(code, group, "narrative of $code", from, to, listOf("", "", "", ""), null, null, Part2Origin.Requested, "00000000")

    private fun day(
        month: Int,
        day: Int,
    ) = CalendarDate(2026, month, day)

    private fun arrange(vararg occurrences: Part2Occurrence) = SlipOrdering.arrange(occurrences.toList())

    private fun codes(order: Part2Order) = order.occurrences.map { it.code }

    @Test
    fun occurrencesComeOutInDateOrderWhateverOrderTheyWereDraftedIn() {
        val orders = arrange(occurrence("LATE", "A", day(9, 5)), occurrence("EARLY", "A", day(7, 1)), occurrence("MIDDLE", "A", day(7, 5)))
        assertEquals(listOf(listOf("EARLY", "MIDDLE", "LATE")), orders.map(::codes))
    }

    @Test
    fun aTieKeepsTheDraftedOrderSoTheRequestedCodeStaysAheadOfItsLinks() {
        val orders = arrange(occurrence("FIRST", "A", day(7, 5)), occurrence("SECOND", "A", day(7, 5)), occurrence("THIRD", "A", day(7, 5)))
        assertEquals(listOf("FIRST", "SECOND", "THIRD"), codes(orders.single()))
    }

    @Test
    fun anOccurrenceWithOnlyAToDateSortsByThatAndOneWithNoDateGoesLast() {
        val orders = arrange(occurrence("NODATE", "A", null), occurrence("ONLYTO", "A", null, day(7, 2)), occurrence("DATED", "A", day(7, 3)))
        assertEquals(listOf("ONLYTO", "DATED", "NODATE"), codes(orders.single()))
    }

    @Test
    fun aGroupBClaimIsSplitIntoItsOwnOrderEvenWhenItIsTheEarliest() {
        // why: para 7.1 - Group B claims are never combined with A/AM/C/CM.
        val orders = arrange(occurrence("CLAIM", "B", day(7, 1)), occurrence("MONEY", "A", day(7, 5)), occurrence("NOMONEY", "C", day(7, 6)))
        assertEquals(listOf(listOf("MONEY", "NOMONEY"), listOf("CLAIM")), orders.map(::codes))
        assertEquals(listOf(OrderFamily.FINANCIAL, OrderFamily.CLAIM), orders.map { it.family })
    }

    @Test
    fun eachClaimGetsItsOwnOrderInDateOrder() {
        // why: owner decision (2026-10-23, from practice): Group B claims were always single-occurrence orders, so two claims are two orders.
        val orders = arrange(occurrence("CLAIMB", "B", day(8, 1)), occurrence("CLAIMA", "B", day(7, 1)))
        assertEquals(listOf(listOf("CLAIMA"), listOf("CLAIMB")), orders.map(::codes))
        assertEquals(listOf(OrderFamily.CLAIM, OrderFamily.CLAIM), orders.map { it.family })
    }

    @Test
    fun theOrderHeaderListsEachGroupLetterPresentOnceInTheManualsOrderAndInfersNone() {
        // why: real orders print "GROUP 'A'/'AM'/'C'" for one order; the letters are those of the occurrences, never a guess.
        val mixed = arrange(occurrence("X", "C", day(7, 1)), occurrence("Y", "A", day(7, 2)), occurrence("Z", "AM", day(7, 3)), occurrence("W", "A", day(7, 4)))
        assertEquals(listOf("A", "AM", "C"), mixed.single().groups)
        assertEquals(listOf("CM"), arrange(occurrence("X", "CM", day(7, 1))).single().groups)
        assertEquals(emptyList(), arrange(occurrence("NOGROUP", null, day(7, 1))).single().groups, "a serial with no printed group adds no letter")
        assertEquals(listOf(listOf("A"), listOf("B")), arrange(occurrence("Y", "A", day(7, 2)), occurrence("CLAIM", "B", day(7, 1))).map { it.groups })
    }

    @Test
    fun theOrderIsFinancialWhenAnyOccurrenceIsAOrAM() {
        // why: the order group is A/AM when any occurrence is A/AM (the plan's para 6.4 rule), however many non-financial ones join it.
        assertEquals(OrderFamily.FINANCIAL, arrange(occurrence("X", "C", day(7, 1)), occurrence("Y", "A", day(7, 2))).single().family)
        assertEquals(OrderFamily.FINANCIAL, arrange(occurrence("X", "CM", day(7, 1)), occurrence("Y", "AM", day(7, 2))).single().family)
        assertEquals(OrderFamily.NON_FINANCIAL, arrange(occurrence("X", "C", day(7, 1)), occurrence("Y", "CM", day(7, 2))).single().family)
    }

    @Test
    fun aSerialWithNoPrintedGroupJoinsTheOrderButNeverDecidesItsFamily() {
        // why: no group is ever inferred (Round 8), so a no-group occurrence alone is UNGROUPED and beside others it adds nothing.
        assertEquals(OrderFamily.UNGROUPED, arrange(occurrence("NOGROUP", null, day(7, 1))).single().family)
        val mixed = arrange(occurrence("NOGROUP", null, day(7, 1)), occurrence("Y", "C", day(7, 2))).single()
        assertEquals(OrderFamily.NON_FINANCIAL, mixed.family)
        assertEquals(listOf("NOGROUP", "Y"), codes(mixed))
    }

    @Test
    fun twoIdenticalRowsAreOneRow() {
        // why: two requests of one event can link to the same order (for example the same ration stop); the clerk must not type it twice.
        val orders = arrange(occurrence("SAME", "A", day(7, 1)), occurrence("SAME", "A", day(7, 1)), occurrence("SAME", "A", day(7, 2)))
        assertEquals(listOf("SAME", "SAME"), codes(orders.single()), "only an identical row is merged; a different date is a different occurrence")
    }

    @Test
    fun nothingDraftedMeansNoOrders() {
        assertEquals(emptyList(), arrange())
    }
}
