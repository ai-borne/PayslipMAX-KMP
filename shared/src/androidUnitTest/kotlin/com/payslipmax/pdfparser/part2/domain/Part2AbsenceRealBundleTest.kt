package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Re-grants after an absence that is not a leave order (Phase 12, plan item 49; DECISIONS Round 44), on the real shipped templates and rules.
 * The manual re-grants HAA (p.49 note ii), SCCIA (p.53 note 2) and the field-area allowances (p.55 note 2) on rejoining from hospital or a course
 * too, so an officer back from either must be able to draft them on their own; after a hospital stay of fewer than 15 days the field-area
 * allowance is not re-granted, which the slip warns about. Synthetic dates and invented stations, Corps and notifications throughout.
 */
class Part2AbsenceRealBundleTest {
    private val absence = mapOf("leave_from" to date(10, 1), "leave_to" to date(10, 20))

    private fun own(
        code: String,
        details: Map<String, SlotValue>,
        values: Map<String, SlotValue> = absence,
    ) = OccurrenceRequest(code, details + values)

    private fun details(code: String) =
        when (code) {
            in AreaRules.haaCodes -> AreaAnswers.haa
            in AreaRules.scciaCodes -> AreaAnswers.sccia
            else -> AreaAnswers.field
        }

    private fun findings(request: OccurrenceRequest) = RealPart2.draft(request).findings

    @Test
    fun everyHaaSccaAndFieldCodeDraftsOnItsOwnFromTheDayAfterTheAbsence() {
        for (code in AreaRules.haaCodes + AreaRules.scciaCodes + AreaRules.fieldCodes) {
            val rows = RealPart2.rows(RealPart2.draft(own(code, details(code))))

            assertEquals(listOf(code), rows.map { it.code }, "$code is the one row")
            assertEquals(CalendarDate(2026, 10, 21), rows.single().from, "$code is dated the day after the last day away")
        }
    }

    @Test
    fun theSuffixIsCountedSoTheRejoiningDayFollowsTheLastDayAway() {
        val request = own("GTDCIAFD", AreaAnswers.sccia, absence + mapOf("suffix_last" to date(10, 22)))

        assertEquals(CalendarDate(2026, 10, 23), RealPart2.rows(RealPart2.draft(request)).single().from)
    }

    @Test
    fun aFieldAreaDraftForFewerThan15DaysCarriesTheP55Warning() {
        for (code in AreaRules.fieldCodes) {
            val finding = findings(own(code, AreaAnswers.field, mapOf("leave_from" to date(10, 1), "leave_to" to date(10, 14)))).single()

            assertEquals(RuleFinding("field_short_absence", RuleSeverity.WARN, code, LinkBasis.MANUAL, 55, "2"), finding, code)
        }
    }

    @Test
    fun fifteenDaysAwayDraftsTheFieldAreaRegrantWithNoWarning() {
        for (code in AreaRules.fieldCodes) {
            assertEquals(emptyList(), findings(own(code, AreaAnswers.field, mapOf("leave_from" to date(10, 1), "leave_to" to date(10, 15)))), code)
        }
    }

    @Test
    fun aShortAbsenceDraftsHaaAndSccia_WithNoWarning_BecauseTheirNotesNameNoHospitalException() {
        for (code in AreaRules.haaCodes + AreaRules.scciaCodes) {
            assertEquals(emptyList(), findings(own(code, details(code), mapOf("leave_from" to date(10, 1), "leave_to" to date(10, 3)))), code)
        }
    }

    @Test
    fun theFieldCodeLinkedFromAShortAnnualLeaveStaysSilent() {
        // why: annual leave of any length re-grants the field allowance (only casual leave under 15 days does not); the warning is for a stand-alone draft.
        val request = AreaAnswers.leave("GTDAL", 10 to 5, 10 to 14, AreaAnswers.field("GTDHAFA"))

        assertTrue("GTDHAFA" in AreaAnswers.codes(request), "the field re-grant is linked")
        assertEquals(emptyList(), findings(request).filter { it.ruleId == "field_short_absence" })
    }

    @Test
    fun theLeaveRibbonHomesTheAreaCodesInACaseOfTheirOwn() {
        val ribbon = RealPart2.section.ribbons.first { it.id == "leave" }
        val case = ribbon.cases.first { it.id == "leave_absence" }

        assertEquals("Area allowances after hospital or a course", case.title)
        assertEquals(listOf("3.13", "3.14", "3.15", "3.19", "3.20", "3.21", "3.22", "3.23", "3.24", "3.25", "3.26"), case.serials)
        assertEquals(emptyList(), case.lookup, "the two stop orders are future drafts of the Field ribbon, so they are not marked look-up only")
        val homes = RealPart2.section.ribbons.flatMap { r -> r.cases.flatMap { c -> c.serials } }
        for (serial in case.serials) assertEquals(1, homes.count { it == serial }, "$serial has exactly one home")
    }

    @Test
    fun theNewCaseOffersADraftForEachOfTheNineRegrantCodes() {
        val rows = Part2LookupIndex(RealPart2.section).case("leave_absence")!!.rows

        assertEquals(AreaRules.haaCodes + AreaRules.scciaCodes + AreaRules.fieldCodes, rows.mapNotNull { it.draftCode })
        assertEquals(emptyList(), rows.filter { it.lookupOnly }.map { it.serial })
        assertEquals(listOf(null, null), rows.filter { it.serial in listOf("3.22", "3.26") }.map { it.draftCode }, "the two stop orders have no template yet, so they offer no draft")
    }
}
