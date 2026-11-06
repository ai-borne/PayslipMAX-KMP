package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a hospital stay whose absence is never regularised by Part II orders can cost the officer pay, so the Bank gently notes a stay that
 * has ended but has no covering regularising entry. The cost of a wrong answer is asymmetric: a false flag nags, a missed one hides a
 * recovery, so every one of the 16 worked cases must pass once its regularising entry is there and fail without it, and a stay that has
 * not ended is never flagged. Dates are invented or date-only; no order number, name or payslip figure appears.
 */
class HospitalStaysTest {
    private val today = date(2025, 12, 1)

    private fun d(text: String): CalendarDate = text.split("-").let { CalendarDate(it[0].toInt(), it[1].toInt(), it[2].toInt()) }

    /** "CODE from" or "CODE from to" with ISO dates. */
    private fun e(
        id: String,
        spec: String,
    ): BankEntry {
        val p = spec.split(" ")
        return BankEntry(id, p[0], d(p[1]), p.getOrNull(2)?.let { d(it) }, listOf("", "", "", ""), "", null, 0, 0)
    }

    private fun entries(vararg specs: String) = specs.mapIndexed { i, s -> e("e$i", s) }

    private fun check(
        list: List<BankEntry>,
        now: CalendarDate = today,
    ) = HospitalStays.check(list, now)

    /** One of the 16 worked cases: the stay's own casualties, then the regularising ones. */
    private class Case(val stay: List<String>, val regularising: List<String>)

    private val cases =
        listOf(
            Case(listOf("HOSADM 2025-01-01", "HOSDISCH 2025-01-29", "RPDUHSP 2025-01-30"), listOf("SLC 2025-01-01 2025-01-29")),
            Case(listOf("HOSADM 2024-12-20", "HOSDISCH 2025-01-16", "RPDUHSP 2025-01-17"), listOf("SLC 2024-12-20 2025-01-16")),
            Case(sick("2025-01-01", "2025-01-11", "2025-01-12", "2025-01-26", "2025-01-27", "2025-01-28"), listOf("SLC 2025-01-01 2025-01-27")),
            Case(sick("2024-12-20", "2025-01-01", "2025-01-02", "2025-01-15", "2025-01-17", "2025-01-18"), listOf("SLC 2024-12-20 2025-01-17")),
            Case(plain("2025-01-01", "2025-05-30", "2025-05-31"), listOf("REGALMS 2025-01-01 2025-05-30", "GTDALHOS 2025-01-01 2025-03-01")),
            Case(plain("2024-10-20", "2025-04-04", "2025-04-05"), listOf("REGALMS 2024-10-20 2025-04-04")),
            Case(sick("2025-01-01", "2025-01-01", "2025-01-02", "2025-01-15", "2025-05-30", "2025-05-31"), listOf("REGALMS 2025-01-01 2025-05-30")),
            Case(sick("2024-10-01", "2024-10-10", "2024-10-11", "2025-02-06", "2025-03-13", "2025-03-14"), listOf("REGALMS 2024-10-01 2025-03-13")),
            Case(plain("2025-01-01", "2025-05-30", "2025-05-31"), listOf("REGAL 2025-01-01 2025-05-30")),
            Case(plain("2024-10-25", "2025-03-09", "2025-03-10"), listOf("REGAL 2024-10-25 2025-03-09")),
            Case(sick("2025-05-14", "2025-05-19", "2025-05-20", "2025-09-20", "2025-11-04", "2025-11-05"), listOf("REGAL 2025-05-14 2025-11-04")),
            Case(sick("2024-10-01", "2024-10-10", "2024-10-11", "2025-02-06", "2025-03-13", "2025-03-14"), listOf("REGAL 2024-10-01 2025-03-13")),
            Case(plain("2025-01-01", "2025-08-09", "2025-08-10"), listOf("REGAL 2025-01-01 2025-08-09", "HOSFURPY 2025-06-30 2025-08-09")),
            Case(plain("2024-10-25", "2025-06-07", "2025-06-08"), listOf("REGAL 2024-10-25 2025-06-07")),
            Case(sick("2025-01-01", "2025-01-01", "2025-01-02", "2025-07-08", "2025-07-09", "2025-07-10"), listOf("REGAL 2025-01-01 2025-07-09")),
            // Type 16 (absence over 6 months that spills into a second year, sick leave granted): the dates are the letter's own, date-only.
            Case(
                sick("2024-10-20", "2024-10-25", "2024-10-26", "2025-07-02", "2025-07-06", "2025-07-07"),
                listOf("REGAL 2024-10-20 2025-07-06"),
            ),
        )

    private fun plain(
        adm: String,
        disch: String,
        duty: String,
    ) = listOf("HOSADM $adm", "HOSDISCH $disch", "RPDUHSP $duty")

    private fun sick(
        adm: String,
        sld: String,
        gtd: String,
        re: String,
        disch: String,
        duty: String,
    ) = listOf("HOSADM $adm", "SLDISCH $sld", "GTDSL $gtd", "SLHOSADM $re", "HOSDISCH $disch", "RPDUHSP $duty")

    @Test
    fun allSixteenWorkedCasesPassOnceTheirRegularisingEntryIsThere() {
        assertEquals(16, cases.size)
        cases.forEachIndexed { i, c ->
            assertTrue(check(entries(*(c.stay + c.regularising).toTypedArray())).unregularised.isEmpty(), "type ${i + 1} should pass")
        }
    }

    @Test
    fun eachWorkedCaseIsFlaggedWhenItsRegularisingEntryIsMissing() {
        cases.forEachIndexed { i, c ->
            val result = check(entries(*c.stay.toTypedArray()))
            assertEquals(1, result.unregularised.size, "type ${i + 1} should be noted")
        }
    }

    @Test
    fun aStayThatHasNotEndedIsNeverFlagged() {
        assertTrue(check(entries("HOSADM 2025-10-01")).unregularised.isEmpty())
        assertTrue(check(entries("HOSADM 2025-10-01", "SLDISCH 2025-10-10", "GTDSL 2025-10-11")).unregularised.isEmpty())
    }

    @Test
    fun aRegularisingEntryMustStartOnOrBeforeTheAdmissionAndEndOnOrAfterIt() {
        val stay = arrayOf("HOSADM 2025-03-10", "HOSDISCH 2025-03-20")
        assertTrue(check(entries(*stay, "SLC 2025-03-10 2025-03-20")).unregularised.isEmpty(), "starts on the admission day")
        assertTrue(check(entries(*stay, "SLC 2025-03-01 2025-03-10")).unregularised.isEmpty(), "ends on the admission day")
        assertTrue(check(entries(*stay, "SLC 2025-03-01")).unregularised.isEmpty(), "no To means it runs on")
        assertEquals(1, check(entries(*stay, "SLC 2025-03-11 2025-03-20")).unregularised.size, "starts a day after")
        assertEquals(1, check(entries(*stay, "SLC 2025-03-01 2025-03-09")).unregularised.size, "ends a day before")
    }

    @Test
    fun anUnrelatedCasualtyDoesNotRegularise() {
        assertEquals(1, check(entries("HOSADM 2025-03-10", "HOSDISCH 2025-03-20", "GTDSL 2025-03-01")).unregularised.size)
    }

    @Test
    fun aFinalDischargeAloneOrAReportForDutyAloneEndsTheStay() {
        assertEquals(1, check(entries("HOSADM 2025-03-10", "HOSDISCH 2025-03-20")).unregularised.size)
        assertEquals(1, check(entries("HOSADM 2025-03-10", "RPDUHSP 2025-03-21")).unregularised.size)
    }

    @Test
    fun anIntermediaryDischargeDoesNotEndTheStay() {
        assertTrue(check(entries("HOSADM 2025-03-10", "SLDISCH 2025-03-15")).unregularised.isEmpty())
    }

    @Test
    fun entriesInAnyOrderGiveTheSameAnswer() {
        val list = entries("RPDUHSP 2025-03-21", "HOSDISCH 2025-03-20", "SLC 2025-03-10 2025-03-20", "HOSADM 2025-03-10")
        assertEquals(check(list), check(list.reversed()))
        assertTrue(check(list).unregularised.isEmpty())
    }

    @Test
    fun anEmptyBankHasNothingToNote() {
        assertTrue(check(emptyList()).unregularised.isEmpty())
        assertTrue(check(emptyList()).timeBarred.isEmpty())
    }

    @Test
    fun withSeveralStaysOnlyTheUnregularisedOneIsNoted() {
        val list =
            entries(
                "HOSADM 2025-01-01",
                "HOSDISCH 2025-01-20",
                "SLC 2025-01-01 2025-01-20",
                "HOSADM 2025-06-01",
                "HOSDISCH 2025-06-20",
            )
        val result = check(list)
        assertEquals(setOf("e3"), result.unregularised)
    }

    @Test
    fun aLaterStaysDischargeDoesNotEndAnEarlierOpenStay() {
        val list = entries("HOSADM 2025-01-01", "HOSADM 2025-06-01", "HOSDISCH 2025-06-20", "SLC 2025-06-01 2025-06-20")
        assertTrue(check(list).unregularised.isEmpty(), "the first stay is still open; the second is regularised")
    }

    @Test
    fun anAdmissionExactlyTwoYearsAgoIsNotTimeBarredButOneDayMoreIs() {
        val stay = entries("HOSADM 2023-12-01", "HOSDISCH 2023-12-20")
        val exactly = check(stay, date(2025, 12, 1))
        assertEquals(1, exactly.unregularised.size)
        assertTrue(exactly.timeBarred.isEmpty())
        val over = check(stay, date(2025, 12, 2))
        assertEquals(setOf("e0"), over.timeBarred)
    }

    @Test
    fun aLeapDayAdmissionTurnsTwoYearsOldOnTheLastDayOfFebruary() {
        val stay = entries("HOSADM 2024-02-29", "HOSDISCH 2024-03-10")
        assertTrue(check(stay, date(2026, 2, 28)).timeBarred.isEmpty())
        assertFalse(check(stay, date(2026, 3, 1)).timeBarred.isEmpty())
    }

    @Test
    fun aRegularisedStayIsNeverTimeBarred() {
        val list = entries("HOSADM 2020-01-01", "HOSDISCH 2020-01-20", "SLC 2020-01-01 2020-01-20")
        assertTrue(check(list).timeBarred.isEmpty())
    }

    @Test
    fun theResultHidesItsContentsInToString() {
        val text = check(entries("HOSADM 2025-01-01", "HOSDISCH 2025-01-20")).toString()
        assertFalse(text.contains("e0") || text.contains("2025"), text)
    }
}
