package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.AreaAnswers.codes
import com.payslipmax.pdfparser.part2.domain.AreaAnswers.leave
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.rows
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden slips for the Transport Allowance (Part II plan, phase 9; ODP-2026 p.70 serials 3.53 and 3.54, p.71 notes iii and v): STPTRAN and a
 * re-grant under the officer's own category, drafted only when the absence covers a full calendar month, dated by the months lost (handbook,
 * Pay & Allowances 2023, pp.124-125), with the reason in columns 3 and 7. Real shipped templates, synthetic dates only.
 */
class Part2BatchFourTransportTest {
    private val reason = "absence on leave covering a full calendar month"
    private val grantReason = "rejoining from leave after an absence covering a full calendar month"

    private fun day(
        month: Int,
        day: Int,
    ) = CalendarDate(2026, month, day)

    private fun al(
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        vararg answers: String,
        extra: Map<String, SlotValue> = emptyMap(),
    ) = rows(RealPart2.draft(leave("GTDAL", from, to, *answers, extra = extra)))

    @Test
    fun anAbsenceFrom05JulTo03AugCoversNoWholeMonthSoNoTransportRowIsDrafted() {
        assertEquals(listOf("GTDAL"), codes(leave("GTDAL", 7 to 5, 8 to 3, AreaAnswers.tran("GTDTRAN1"))))
    }

    @Test
    fun anAbsenceFrom25JunTo05AugStopsTheAllowanceFromJulyAndGrantsItFromAugust() {
        val rows = al(6 to 25, 8 to 5, AreaAnswers.tran("GTDTRAN1"))
        assertEquals(listOf("GTDAL" to day(6, 25), "STPTRAN" to day(7, 1), "GTDTRAN1" to day(8, 1)), rows.map { it.code to it.from }, "date order: the leave, the stop, the grant")
        val stop = rows[1]
        assertEquals("Ceased to draw Transport Allowance from 01/07/2026 due to $reason", stop.narrative)
        assertEquals(listOf("", reason, "Testville", ""), stop.data, "Data 1 and 4 are not required; the reason is in column 7 and the station in column 8")
        assertNull(stop.to)
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 71, "v"), stop.origin)
        val grant = rows[2]
        assertEquals("Granted Transport Allowance – 1 (at lower rate) at Testville from 01/08/2026 on account of $grantReason", grant.narrative)
        assertEquals(listOf("", grantReason, "Testville", ""), grant.data, "category 1 needs no Data 4")
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 71, "iii"), grant.origin)
        assertEquals("A", grant.group)
    }

    @Test
    fun anAbsenceEndingOnTheLastDayOfAMonthIsGrantedFromTheNextDay() {
        val rows = al(6 to 25, 7 to 31, AreaAnswers.tran("GTDTRAN2"))
        assertEquals(listOf("GTDAL" to day(6, 25), "STPTRAN" to day(7, 1), "GTDTRAN2" to day(8, 1)), rows.map { it.code to it.from })
        assertTrue("Granted Transport Allowance – 2 (at higher rate) at Testville from 01/08/2026" in rows.last().narrative)
    }

    @Test
    fun theDisabilityCategoriesPrintTheNatureOfTheDisabilityInColumns3And9() {
        // why: p.71 note iv - "Nature of disability to be indicated in Column No. 3 and 9"; the table's Data 4 for Cat-3 and 4.
        for ((code, words) in mapOf("GTDTRAN3" to "3 (With disability at lower rate)", "GTDTRAN4" to "4 (With disability at higher rate)")) {
            val grant = al(6 to 25, 8 to 5, AreaAnswers.tran(code)).last()
            assertEquals(code, grant.code)
            assertEquals("Granted Transport Allowance – $words at Testville from 01/08/2026 on account of $grantReason, disability: hearing impairment", grant.narrative, code)
            assertEquals("hearing impairment", grant.data[3], code)
            assertTrue("hearing impairment and will be entitled to Transport Allowance at double the normal rate" in grant.certificate.orEmpty(), code)
        }
    }

    @Test
    fun theMajorGeneralCategoryPrintsTheRankInColumns3And7AndInData4() {
        // why: the table says Data 4 is "Rank of the Officer" for Cat-5 and note vi says "Column No. 3 and 7"; both are printed, so both are followed.
        val grant = al(6 to 25, 8 to 5, AreaAnswers.tran("GTDTRAN5")).last()
        assertEquals("Granted Transport Allowance – 5 (For Maj Gen and above) at Testville from 01/08/2026 on account of $grantReason, rank Maj Gen", grant.narrative)
        assertEquals(listOf("", "$grantReason, rank Maj Gen", "Testville", "Maj Gen"), grant.data)
        val certificate = grant.certificate.orEmpty()
        assertTrue("Level 14 and above in Pay Matrix" in certificate && "withdrawn from the date" in certificate, certificate)
    }

    @Test
    fun theCertificateOfTheLowerCategoriesQuotesTheMinistryLettersAndTheNoGovernmentTransportLine() {
        val certificate = al(6 to 25, 8 to 5, AreaAnswers.tran("GTDTRAN1")).last().certificate.orEmpty()
        assertTrue("Transport Allowance Certificates with effect from 01/07/2017" in certificate, certificate)
        assertTrue("12630/Tpt.A/Mov C/246/D(Mov)/17 dated 15 Sept 2017 have been fulfilled" in certificate, certificate)
        assertTrue("stationed at Testville (city) on Permanent Posting" in certificate, certificate)
        assertTrue("not provided with Govt. transport" in certificate, certificate)
        assertTrue("double the normal rate" !in certificate && "Level 14" !in certificate, "the disability and Level 14 lines belong to other categories")
    }

    @Test
    fun aPrefixAndSuffixCountAsAbsenceWhenWorkingOutTheMonthsLost() {
        // why: Round 11 - the absence runs from the first prefix day to the last suffix day.
        val values = mapOf("prefix_first" to date(6, 30), "suffix_last" to date(7, 31))
        assertEquals(listOf("STPTRAN", "GTDAL", "GTDTRAN1"), al(7 to 2, 7 to 29, AreaAnswers.tran("GTDTRAN1"), extra = values).map { it.code }, "the stop is dated 1 July, before the leave starts on 2 July")
        assertEquals(listOf("GTDAL"), al(7 to 2, 7 to 29, AreaAnswers.tran("GTDTRAN1")).map { it.code }, "without them July is not whole")
    }

    @Test
    fun noCategoryAnsweredMeansNoTransportRowsAndAnyOtherLeaveWorksToo() {
        assertEquals(listOf("GTDAL"), al(6 to 25, 8 to 5).map { it.code })
        assertEquals(listOf("GTDCL", "STPTRAN", "GTDTRAN1"), codes(leave("GTDCL", 6 to 25, 8 to 5, AreaAnswers.tran("GTDTRAN1"))), "the rule reads the dates, not the kind of leave")
    }
}
