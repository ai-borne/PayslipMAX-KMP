package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RealPart2.annualLeave
import com.payslipmax.pdfparser.part2.domain.RealPart2.answers
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.draft
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.rows
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden slips for content batch 1 (Part II plan, phase 2b), on the real shipped templates and synthetic dates only. They pin the
 * behaviour the owner approved: what an annual or casual leave drags with it (ration, R&H), what each row prints and which of the
 * manual's notes fire. A narrative typo found in the owner's review is fixed in the template JSON and in the expectation here, in
 * the same commit.
 */
class Part2BatchOneGoldenTest {
    private val ration = RationRules.ANSWER_ON_RATION
    private val rh = AreaRules.ANSWER_RH_PREFIX + "GTDRH12"

    private fun annual(vararg answer: String) = draft(OccurrenceRequest("GTDAL", annualLeave, answers(*answer)))

    private fun day(
        month: Int,
        day: Int,
    ) = CalendarDate(2026, month, day)

    @Test
    fun thirtyDaysOfAnnualLeaveWithAPrefixOnRationInAnRhStationIsOneAGroupOrderInDateOrder() {
        val slip = annual(ration, rh)
        val order = slip.orders.single()
        assertEquals(OrderFamily.FINANCIAL, order.family, "one order, financial because most of it is group A")
        assertEquals(listOf("A", "C"), order.groups, "the header carries each group letter once: SORS and TORS are C")
        // SORS + GTDRSHNA + STPRH from the first prefix day; TORS + STPRSHNA + the re-grant from rejoining (the day after 04 Aug).
        assertEquals(
            listOf("SORS" to day(7, 4), "GTDRSHNA" to day(7, 4), "STPRH" to day(7, 4), "GTDAL" to day(7, 6), "TORS" to day(8, 5), "STPRSHNA" to day(8, 5), "GTDRH12" to day(8, 5)),
            order.occurrences.map { it.code to it.from },
        )
        assertEquals(listOf("C", "A", "A", "A", "C", "A", "A"), order.occurrences.map { it.group })
        assertEquals(emptyList(), slip.findings, "30 days with a 2-day prefix breaks no note")
        assertEquals(emptyList(), slip.undrafted)
    }

    @Test
    fun theAnnualLeaveRowPrintsTheManualsNarrativeAndItsColumns() {
        val row = rows(annual()).single()
        assertEquals("GTDAL", row.code)
        assertEquals("Granted 30 days Annual Leave for the year 2026 from 06/07/2026 to 04/08/2026 with permission to prefix 04/07/2026 & suffix Nil", row.narrative)
        assertEquals(day(7, 6) to day(8, 4), row.from to row.to, "From and To exclude the prefix (note 2, p.118)")
        assertEquals(listOf("04/07/2026", "", "30", "2026"), row.data, "Data 1 first prefix day, Data 2 last suffix day, Data 3 days, Data 4 year")
        assertEquals(Part2Origin.Requested, row.origin)
    }

    @Test
    fun theRationFourAndRhRowsPrintTheirOwnWordsAndCertificates() {
        val byCode = rows(annual(ration, rh)).associateBy { it.code }
        assertEquals("Struck of Ration Strength w.e.f. 04/07/2026", byCode.getValue("SORS").narrative)
        assertEquals("Taken on Ration Strength w.e.f. 05/08/2026", byCode.getValue("TORS").narrative)
        assertEquals("Granted Ration Allowance from 04/07/2026", byCode.getValue("GTDRSHNA").narrative)
        assertEquals("Ceased to draw Ration Allowance from 05/08/2026", byCode.getValue("STPRSHNA").narrative)
        assertEquals("Ceased to draw Risk & Hardship Allowance from 04/07/2026 for ZZ01", byCode.getValue("STPRH").narrative)
        val grant = byCode.getValue("GTDRH12")
        assertEquals("Granted Risk & Hardship Allowance @ R1H2 rate from 05/08/2026 for ZZ01", grant.narrative)
        assertEquals(listOf("ZZ01", "R1H2", "Alpha, N-17/2026, 02/03/2026", "12"), grant.data)
        assertTrue("The Officer is stationed at ZZ01 w.e.f. 05/08/2026" in grant.certificate.orEmpty(), grant.certificate)
        assertTrue("included at Ser No 12 of Alpha Corps Notification No. N-17/2026 Dated 02/03/2026" in grant.certificate.orEmpty())
        val rmc = byCode.getValue("GTDRSHNA").certificate.orEmpty()
        assertTrue("The officer was on annual leave from 04/07/2026 to 04/08/2026 away from Station HQ" in rmc, rmc)
        assertNull(byCode.getValue("SORS").certificate, "only the grant carries the Ration Money Allowance certificate (note 8, p.99)")
        assertTrue(byCode.getValue("SORS").origin is Part2Origin.Linked && (byCode.getValue("STPRH").origin as Part2Origin.Linked).basis == LinkBasis.PRACTICE)
        assertEquals(LinkBasis.MANUAL, (byCode.getValue("SORS").origin as Part2Origin.Linked).basis)
    }

    @Test
    fun anOfficerNotOnRationStrengthGetsNoRationFourAndOneInNoRhStationGetsNoRhRows() {
        assertEquals(listOf("GTDAL"), rows(annual()).map { it.code })
        assertEquals(listOf("STPRH", "GTDAL", "GTDRH12"), rows(annual(rh)).map { it.code }, "R&H without ration")
        assertEquals(listOf("SORS", "GTDRSHNA", "GTDAL", "TORS", "STPRSHNA"), rows(annual(ration)).map { it.code }, "ration without R&H")
    }

    @Test
    fun theOfficersOwnRhCodeIsTheOneRegrantedWithItsOwnRate() {
        val ninety = rows(draft(OccurrenceRequest("GTDAL", annualLeave, answers(AreaRules.ANSWER_RH_PREFIX + "GTDRH77")))).last()
        assertEquals("GTDRH77", ninety.code)
        assertEquals("R1H2", ninety.narrative.substringAfter("@ 77% ").substringBefore(" rate"))
        assertEquals("77% of R1H2", ninety.data[1])
        assertEquals("60% of R2H2", rows(draft(OccurrenceRequest("GTDAL", annualLeave, answers(AreaRules.ANSWER_RH_PREFIX + "GTDRH60")))).last().data[1])
    }

    @Test
    fun aNineDayCasualLeaveDraftsTheRationFourAndNoRhRowsEvenInAnRhStation() {
        // why: practice - a 9-day CL carried the ration four but no R&H stop; R&H rows link from annual leave only (owner decision).
        val values = annualLeave + mapOf("leave_to" to date(7, 14)) - "prefix_first"
        val slip = draft(OccurrenceRequest("GTDCL", values, answers(ration, rh)))
        assertEquals(listOf("GTDCL", "SORS", "GTDRSHNA", "TORS", "STPRSHNA"), rows(slip).map { it.code })
        assertEquals("Granted 9 days casual Leave for the year 2026 from 06/07/2026 to 14/07/2026 with permission to suffix Nil", rows(slip).first().narrative)
    }

    @Test
    fun aSpecialCasualLeaveAlsoDraftsTheRationFourAndAPrefixMovesTheirStart() {
        val values = annualLeave + mapOf("reason" to text("a family event"), "leave_to" to date(7, 10))
        val slip = draft(OccurrenceRequest("GTDSPCL", values, answers(ration)))
        assertEquals("Granted Special Casual Leave on account of a family event upto 30 days (w.e.f. 06/07/2026 to 10/07/2026)", rows(slip).first { it.code == "GTDSPCL" }.narrative)
        assertEquals(day(7, 4), rows(slip).first { it.code == "SORS" }.from, "ration runs from the prefix (note 3, p.98)")
    }

    @Test
    fun aSuffixPushesTheRejoiningDateAndTheLastDayAwayOut() {
        val values = annualLeave + mapOf("suffix_last" to date(8, 7))
        val byCode = rows(draft(OccurrenceRequest("GTDAL", values, answers(ration)))).associateBy { it.code }
        assertEquals(day(8, 8), byCode.getValue("TORS").from, "the day after the last suffix day")
        assertTrue("to 07/08/2026 away from Station HQ" in byCode.getValue("GTDRSHNA").certificate.orEmpty())
        assertEquals(listOf("04/07/2026", "07/08/2026", "30", "2026"), byCode.getValue("GTDAL").data)
    }

    @Test
    fun theNoteOneExampleCountsSixtyTwoDaysAndIsAcknowledgedAgainstTheSixtyDayCap() {
        // why: note 1 p.115 - 05/07 to 04/09 is 62 days, not 60, so an order for "60 days" over those dates is rejected in audit.
        val slip = draft(OccurrenceRequest("GTDAL", annualLeave + mapOf("leave_from" to date(7, 5), "leave_to" to date(9, 4)) - "prefix_first"))
        assertTrue(rows(slip).single().narrative.startsWith("Granted 62 days Annual Leave"), rows(slip).single().narrative)
        val finding = slip.findings.single()
        assertEquals(Triple("al_year_cap", RuleSeverity.ACK, "GTDAL"), Triple(finding.ruleId, finding.severity, finding.code))
        assertEquals(Triple(LinkBasis.MANUAL, 115, "2"), Triple(finding.basis, finding.page, finding.note))
    }

    @Test
    fun fortyDaysEarlierPlusTwentyFiveNowIsAcknowledgedCitingNoteTwo() {
        val leave = annualLeave + mapOf("leave_to" to date(7, 30), "earlier_al" to number(40)) - "prefix_first"
        val finding = draft(OccurrenceRequest("GTDAL", leave)).findings.single()
        assertEquals(Triple("al_year_cap", RuleSeverity.ACK, 115), Triple(finding.ruleId, finding.severity, finding.page))
        assertEquals(emptyList(), draft(OccurrenceRequest("GTDAL", leave + mapOf("earlier_al" to number(35)))).findings, "35 + 25 is exactly 60")
    }

    @Test
    fun aPrefixAndSuffixOverFiveDaysAreWarnedAboutNotBlocked() {
        val values = annualLeave + mapOf("prefix_first" to date(7, 1), "suffix_last" to date(8, 6))
        val finding = draft(OccurrenceRequest("GTDAL", values)).findings.single()
        assertEquals(Triple("al_affix_limit", RuleSeverity.WARN, 115), Triple(finding.ruleId, finding.severity, finding.page))
        assertEquals("9", finding.note)
    }

    @Test
    fun twoLeaveYearsAreTwoGrantsNotOne() {
        // why: note 10 p.115 - annual leave for two consecutive years granted at once is notified as two separate occurrences.
        val first = annualLeave + mapOf("leave_from" to date(12, 1, 2025), "leave_to" to date(12, 15, 2025), "year" to number(2025)) - "prefix_first"
        val second = annualLeave + mapOf("leave_from" to date(1, 5, 2026), "leave_to" to date(1, 20, 2026)) - "prefix_first"
        val grants = rows(draft(OccurrenceRequest("GTDAL", first), OccurrenceRequest("GTDAL", second)))
        assertEquals(listOf("2025", "2026"), grants.map { it.data[3] })
        assertEquals(listOf("15", "16"), grants.map { it.data[2] })
    }

    @Test
    fun aCasualLeaveBesideAnAnnualLeaveIsAcknowledged() {
        val cl = annualLeave + mapOf("leave_from" to date(9, 1), "leave_to" to date(9, 5)) - "prefix_first"
        val slip = draft(OccurrenceRequest("GTDAL", annualLeave), OccurrenceRequest("GTDCL", cl))
        val finding = slip.findings.single { it.ruleId == "cl_combination" }
        assertEquals(Triple(RuleSeverity.ACK, "GTDCL", 117), Triple(finding.severity, finding.code, finding.page))
    }

    @Test
    fun theLastPartTwoOrderIsCarriedToTheSlipHeader() {
        val last = OrderRef("0099/26", CalendarDate(2026, 6, 30))
        assertEquals(last, draft(OccurrenceRequest("GTDAL", annualLeave), last = last).lastOrder)
    }

    @Test
    fun aMissingLeaveDateIsRefusedByNameNotPrintedBlank() {
        val refused = RealPart2.engine.draft(LeaveEventInput(listOf(OccurrenceRequest("GTDAL", annualLeave - "leave_from"))))
        assertTrue(refused is DraftResult.Refused && refused.issues.any { it.slot == "leave_from" }, "$refused")
    }
}
