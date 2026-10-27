package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.Ch5Answers.codes
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.findings
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.furlough
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.leave
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.on
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.order
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.request
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.rowOf
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the Chapter 5 batch does when its codes meet each other and the ration and cap rules, on the real shipped templates (Part II plan,
 * phase 8): the order of a slip, the ration four and leave of 180 days, the NCC and retirement caps, casual leave beside the new leaves,
 * terminal leave beside annual leave and the reissue warning. Synthetic dates and orders only.
 */
class Part2BatchThreeFlowTest {
    private val ration = "ration"
    private val kind = "leave_kind" to text("leave")
    private val al = leave(on(7, 6), on(8, 4), "year" to number(2026))

    private fun ids(vararg requests: OccurrenceRequest) = findings(*requests).map { it.ruleId }

    @Test
    fun furloughAfterAnnualLeaveIsPublishedAfterItWhateverOrderTheOfficerAsksInAndSoIsTheWholeNote14Order() {
        // why: p.118 note 5, p.119 notes 9 and 14 - annual leave first, then maternity, furlough, child care and EOL; para 6.4 puts them in date order, which is that order.
        val requests =
            listOf(
                request("EOL", leave(on(10, 14), on(10, 31))),
                request("GTDCCL", leave(on(10, 4), on(10, 13), "spell" to text("1st"), "ccl_year_days" to number(10))),
                request("FURLEV", leave(on(9, 4), on(10, 3), "cycle_from" to number(2024), "cycle_to" to number(2026))),
                request("MTRNLV", leave(on(8, 5), on(9, 3), "occasion" to text("First"))),
                request("GTDAL", al),
            )
        assertEquals(listOf("GTDAL", "MTRNLV", "FURLEV", "GTDCCL", "EOL"), codes(*requests.toTypedArray()))
    }

    @Test
    fun theRationFourFollowFurloughAndAreCitedToTheManualsNote18() {
        val rows = RealPart2.rows(RealPart2.draft(request("FURLEV", furlough, ration)))

        assertEquals(listOf("SORS", "GTDRSHNA", "FURLEV", "TORS", "STPRSHNA"), rows.map { it.code }, "p.98 note 3: from the first prefix day, then again from rejoining")
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 98, "1.8"), rows.first { it.code == "SORS" }.origin)
        assertEquals(on(8, 5), rows.first { it.code == "TORS" }.from, "the day after the last day away")
        assertEquals(listOf("FURLEV"), codes(request("FURLEV", furlough)), "not on ration strength: no ration rows")
    }

    @Test
    fun maternityAndChildCareLeaveUnder180DaysDraftTheRationFourCitedToTheirNotes() {
        val maternity = request("MTRNLV", leave(on(11, 1), on(4, 28, 2027), "occasion" to text("First"), kind), ration)
        assertEquals(listOf("MTRNLV", "SORS", "GTDRSHNA", "TORS", "STPRSHNA"), codes(maternity), "179 days: the unit's quarterly order does not apply")
        assertEquals(emptyList(), ids(maternity))

        val care = request("GTDCCL", leave(on(9, 1), on(9, 10), "spell" to text("1st"), "ccl_year_days" to number(10), kind), ration)
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 99, "5"), RealPart2.rows(RealPart2.draft(care)).first { it.code == "SORS" }.origin)
    }

    @Test
    fun leaveOf180DaysOrMoreDraftsNoRationFourAndShowsTheQuarterlyNoteOfPage99() {
        // why: p.99 note 5 - the unit publishes these quarterly in arrears, so a slip that drafted them would pay ration twice.
        val maternity = request("MTRNLV", leave(on(11, 1), on(4, 29, 2027), "occasion" to text("First"), kind), ration)
        assertEquals(listOf("MTRNLV"), codes(maternity))
        val finding = findings(maternity).single()
        assertEquals(RuleFinding("ration_long_leave", RuleSeverity.WARN, "MTRNLV", LinkBasis.MANUAL, 99, "5"), finding)

        val study = request("STDLEV", leave(on(9, 1), on(2, 28, 2027), "qualification" to text("MBA"), "place" to text("Pune"), "liab_years" to number(2), "liab_months" to number(6)), ration)
        assertEquals(listOf("STDLEV"), codes(study))
        assertEquals(listOf("ration_long_leave"), ids(study))
        assertEquals(listOf("CHADPLVE"), codes(request("CHADPLVE", leave(on(9, 1), on(2, 27, 2027), "occasion" to text("First")), ration)), "adoption leave is always 180 days")
        assertEquals(emptyList(), ids(request("MTRNLV", leave(on(11, 1), on(4, 29, 2027), "occasion" to text("First")))), "no ration answer: nothing to leave out")
    }

    @Test
    fun paternityLeaveDraftsNoRationRowsBecauseTheManualNamesNoneForIt() {
        assertEquals(listOf("PTRNLV"), codes(request("PTRNLV", leave(on(9, 1), on(9, 15), "occasion" to text("First")), ration)))
    }

    @Test
    fun theNccAndRetirementCapsReachTheAnnualLeaveCheckThroughTheAnswersAndTheDate() {
        val thirtyOne = leave(on(7, 6), on(8, 5), "year" to number(2026))
        assertEquals(emptyList(), ids(request("GTDAL", thirtyOne)), "31 days is fine for an ordinary officer")
        assertEquals(listOf(RuleFinding("al_year_cap", RuleSeverity.ACK, "GTDAL", LinkBasis.MANUAL, 115, "2")), findings(request("GTDAL", thirtyOne, LeaveRules.ANSWER_NCC_REEMPLOYED)))
        val february = thirtyOne + ("retire_on" to date(2, 10))
        assertEquals(listOf("al_year_cap"), ids(request("GTDAL", february)), "retiring on 10 February 2026: 30 days")
        assertEquals(emptyList(), ids(request("GTDAL", thirtyOne + ("retire_on" to date(3, 1)))), "retiring in March: the full 60")
        assertEquals(listOf("al_year_cap"), ids(request("EXTNAL", leave(on(8, 5), on(9, 3), "year" to number(2026), "earlier_al" to number(31), "grant" to order))), "an extension counts the year's earlier leave")
    }

    @Test
    fun furloughForAnNccOrReEmployedOfficerIsAcknowledged() {
        assertEquals(listOf(RuleFinding("furlough_not_ncc", RuleSeverity.ACK, "FURLEV", LinkBasis.MANUAL, 119, "7")), findings(request("FURLEV", furlough, LeaveRules.ANSWER_NCC_REEMPLOYED)))
        assertEquals(emptyList(), ids(request("FURLEV", furlough)))
    }

    @Test
    fun casualLeaveBesideFurloughIsAcknowledgedButBesideSpecialCasualLeaveIsNot() {
        val casual = request("GTDCL", leave(on(10, 1), on(10, 5), "year" to number(2026)))
        assertEquals(listOf("cl_combination"), ids(casual, request("FURLEV", furlough)))
        assertEquals(emptyList(), ids(casual, request("GTDSPCL", leave(on(10, 6), on(10, 8), "year" to number(2026), "reason" to text("a family event")))))
    }

    @Test
    fun terminalLeaveBesideAnnualLeaveStartsWhereTheAnnualLeaveStartsAndCountsOnlyItsOwn28() {
        val annual = request("GTDAL", leave(on(10, 1), on(10, 30), "year" to number(2026)))
        assertEquals(emptyList(), ids(annual, request("TERLEV", leave(on(10, 1), on(11, 27)))), "p.119 note 10 as the manual words it")
        assertEquals(listOf("terminal_leave_28", "terminal_with_annual"), ids(annual, request("TERLEV", leave(on(10, 31), on(11, 27)))).sorted(), "a stand-alone grant after the annual leave breaks both")
        assertEquals(listOf("terminal_leave_28"), ids(request("TERLEV", leave(on(10, 1), on(10, 27)))), "27 days alone")
    }

    @Test
    fun anExtensionOrReturnThatTheOfficerSaysMovedEarlierAllowancesPointsAtParaEleven() {
        val extension = leave(on(8, 5), on(8, 14), "year" to number(2026), "grant" to order)
        val volrej = mapOf("leave_type" to text("annual leave"), "rejoined_on" to date(8, 1), "cancel_from" to date(8, 2), "cancel_to" to date(8, 4), "grant" to order)
        val recall = mapOf("leave_type" to text("annual leave"), "recalled_on" to date(8, 1), "cancel_from" to date(8, 2), "cancel_to" to date(8, 4), "grant" to order)
        val furloughExtension = leave(on(8, 5), on(8, 14), "cycle_from" to number(2024), "cycle_to" to number(2026), "grant" to order)
        for ((code, values) in listOf("EXTNAL" to extension, "VOLREJ" to volrej, "RECALL" to recall, "EXTNFUR" to furloughExtension)) {
            assertEquals(emptyList(), ids(request(code, values)), "$code: no answer, no warning")
            val warned = findings(request(code, values, LongLeaveRules.ANSWER_EARLIER_ALLOWANCES)).single { it.ruleId == "reissue_allowances" }
            assertEquals(RuleFinding("reissue_allowances", RuleSeverity.WARN, code, LinkBasis.MANUAL, 8, "11, 12.3"), warned, code)
        }
    }

    @Test
    fun anAnnualLeaveCarriesItsSixteenOlderLinksThenTheFifteenAreaLinksOfPhase9() {
        // why: batch 1 left GTDAL at 16 links (the limit); Phase 9 adds the 15 area re-grants and raises the limit with its reason (Phase 11 item 8).
        val template = checkNotNull(RealPart2.section.templates.firstOrNull { it.code == "GTDAL" })
        assertEquals(31, template.links.size)
        assertEquals(listOf("SORS", "GTDRSHNA", "STPRH"), template.links.take(3).map { it.code }, "the older links stay first and unchanged")
        assertTrue(RealPart2.section.templates.filter { it.code in LeaveRules.OTHER_LEAVE }.all { it.links.size <= 4 + 15 })
        assertEquals("GTDAL", rowOf("GTDAL", request("GTDAL", al)).code)
    }
}
