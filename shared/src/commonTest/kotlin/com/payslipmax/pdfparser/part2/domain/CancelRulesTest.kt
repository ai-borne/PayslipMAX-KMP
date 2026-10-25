package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.ref
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The checks on a cancellation (Part II plan, phase 6c; ODP-2026 pp.226-227). A cancellation that does not quote the order it cancels
 * cannot be applied, and one for an old order needs the time-bar sanction enclosed (notes 4 and 6) or it is queried. Dates are synthetic.
 */
class CancelRulesTest {
    private val today = CalendarDate(2026, 10, 25)

    private fun check(id: String) = assertNotNull(CancelRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == id }, "rule $id")

    private fun barred(
        id: String,
        order: SlotValue?,
        on: CalendarDate? = today,
        code: String = "CANCEL",
    ) = check(id).violated(context(code, values = listOfNotNull(order?.let { CancelRules.CANCELLED to it }).toMap(), today = on))

    private val timeBarIds = listOf(CancelRules.CANCEL_TIME_BARRED, CancelRules.CANCELS_TIME_BARRED)

    @Test
    fun theFamilyOffersExactlyTheChecksTheTemplatesCite() {
        assertEquals(
            setOf("cancel_quoted", "cancel_time_barred", "cancels_quoted", "cancels_time_barred"),
            CancelRules.all.map { it.id }.toSet(),
        )
        assertTrue(CancelRules.all.all { it is CheckRule }, "the cancel family holds checks only")
    }

    @Test
    fun theFamilyNamesTheTwoCodesAndTheOneBoxThatQuotesAVaultOrder() {
        // why: the draft flow and the vault read these ids from here, so a screen never writes a code.
        assertEquals(setOf("CANCEL", "CANCELS"), CancelRules.CODES)
        assertEquals(setOf("cancelled"), CancelRules.VAULT_ORDER_SLOTS)
    }

    @Test
    fun aCancellationThatDoesNotQuoteTheOrderIsWarnedAbout() {
        // why: p.226 note 2 and p.227 note 2 - the number and date of the order being cancelled should invariably be quoted.
        for (id in listOf(CancelRules.CANCEL_QUOTED, CancelRules.CANCELS_QUOTED)) {
            assertTrue(check(id).violated(context("X")), "$id: nothing quoted")
            assertTrue(check(id).violated(context("X", mapOf(CancelRules.CANCELLED to SlotValue.TextValue("0123/26")))), "$id: text is not an order reference")
            assertFalse(check(id).violated(context("X", mapOf(CancelRules.CANCELLED to ref("0123/26")))), "$id: quoted")
        }
    }

    @Test
    fun anOrderMoreThanTwoYearsOldNeedsTheTimeBarSanction() {
        // why: notes 4 and 6 - the sanction may be enclosed where the order being cancelled is more than 2 years old.
        for (id in timeBarIds) {
            assertFalse(barred(id, ref("A", 10, 25, 2024)), "$id: exactly 2 years is not more than 2 years")
            assertTrue(barred(id, ref("A", 10, 24, 2024)), "$id: a day over")
            assertFalse(barred(id, ref("A", 10, 26, 2024)), "$id: a day under")
            assertTrue(barred(id, ref("A", 1, 1, 2020)), "$id: long ago")
            assertFalse(barred(id, ref("A", 6, 20, 2026)), "$id: this year")
        }
    }

    @Test
    fun theTwoYearsAreCountedByTheCalendarAcrossYearEndsAndAcrossLeapDays() {
        for (id in timeBarIds) {
            assertTrue(barred(id, ref("A", 12, 31, 2023), CalendarDate(2026, 1, 1)), "$id: 2 years and a day over a year end")
            assertFalse(barred(id, ref("A", 12, 31, 2023), CalendarDate(2025, 12, 31)), "$id: the anniversary itself")
            assertFalse(barred(id, ref("A", 2, 29, 2024), CalendarDate(2026, 2, 28)), "$id: 29 Feb 2024 turns 2 on 28 Feb 2026")
            assertTrue(barred(id, ref("A", 2, 29, 2024), CalendarDate(2026, 3, 1)), "$id: the day after")
        }
    }

    @Test
    fun withoutADateOrAnOrderOrARealDateNothingIsJudged() {
        // why: a rule must never raise a sanction warning on a guess: no device date, nothing quoted yet, or a half-typed date proves nothing.
        for (id in timeBarIds) {
            assertFalse(barred(id, ref("A", 1, 1, 2020), on = null), "$id: no device date")
            assertFalse(barred(id, null), "$id: no order quoted")
            assertFalse(barred(id, SlotValue.RefValue(OrderRef("A", CalendarDate(0, 0, 0)))), "$id: date not chosen yet")
            assertFalse(barred(id, SlotValue.TextValue("2020")), "$id: not an order reference")
            assertFalse(barred(id, ref("A", 12, 31, 2030)), "$id: an order dated after today is never old")
        }
    }
}
