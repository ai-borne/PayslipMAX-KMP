package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.AreaAnswers.codes
import com.payslipmax.pdfparser.part2.domain.AreaAnswers.leave
import com.payslipmax.pdfparser.part2.domain.RealPart2.annualLeave
import com.payslipmax.pdfparser.part2.domain.RealPart2.rows
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden slips for the HAA, SCCIA and field-area re-grants (Part II plan, phase 9, content batch 4), on the real shipped templates and
 * synthetic dates only. They pin what the officer approved: what each re-grant prints, which leave draws it, the manual's casual-leave
 * exceptions, and that no stop is drafted for these three (Round 9). The Transport Allowance is `Part2BatchFourTransportTest`'s.
 */
class Part2BatchFourGoldenTest {
    private fun day(
        month: Int,
        day: Int,
    ) = CalendarDate(2026, month, day)

    private fun al(vararg answers: String) = rows(RealPart2.draft(OccurrenceRequest("GTDAL", annualLeave + AreaAnswers.everything, answers.toSet())))

    @Test
    fun annualLeaveInAnHaaAreaRegrantsTheOfficersCategoryFromTheRejoiningDayWithItsColumnsAndCertificate() {
        val rows = al(AreaAnswers.haa("GTDHH11"))
        assertEquals(listOf("GTDAL", "GTDHH11"), rows.map { it.code })
        val grant = rows.last()
        assertEquals("Granted HAA for CAT-III @ R1H1 rate from 05/08/2026 for ZZ01", grant.narrative)
        assertEquals(day(8, 5) to null, grant.from to grant.to, "From is the day after the last day away; To is not required")
        assertEquals(listOf("ZZ01", "R1H1", "Alpha, N-17/2026, 02/03/2026", "12"), grant.data, "Data 1 the station code, Data 2 the rate, Data 3 the Corps notification, Data 4 its serial")
        assertEquals("A", grant.group)
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 49, "ii"), grant.origin, "cited to the manual, never to practice")
        val certificate = grant.certificate.orEmpty()
        assertTrue("HAA ALLOWANCE ON POSTING / TEMPORARY DUTY" in certificate, certificate)
        assertTrue("stationed at ZZ01" in certificate && "w.e.f. 05/08/2026" in certificate, certificate)
        assertTrue("included at Ser No. 12 of Alpha Corps Notification No. N-17/2026 dated 02/03/2026" in certificate, certificate)
    }

    @Test
    fun eachHaaCategoryPrintsItsOwnRateAndOnlyItsOwnCategoryLineOfTheCertificate() {
        // why: the certificate says "mark whichever is applicable"; the code decides the category, so the other two lines must not print.
        val expected = mapOf("GTDHH11" to Triple("CAT-III @ R1H1", "R1H1", "Category III"), "GTDHH31" to Triple("CAT-II @ R3H1", "R3H1", "Category II"), "GTDHH32" to Triple("CAT-I @ R3H2", "R3H2", "Category I"))
        for ((code, words) in expected) {
            val grant = al(AreaAnswers.haa(code)).last()
            assertEquals(code, grant.code)
            assertEquals("Granted HAA for ${words.first} rate from 05/08/2026 for ZZ01", grant.narrative, code)
            assertEquals(words.second, grant.data[1], code)
            val certificate = grant.certificate.orEmpty()
            assertTrue("(${words.third})" in certificate, "$code: $certificate")
            assertEquals(1, listOf("(Category I)", "(Category II)", "(Category III)").count { it in certificate }, "$code names one category only")
        }
    }

    @Test
    fun aCasualLeaveOf13DaysDraftsNoHaaRegrantAndOneOf14Does() {
        // why: p.49 note ii - "except when he rejoins from Casual Leave of less than 14 days".
        assertEquals(listOf("GTDCL"), codes(leave("GTDCL", 7 to 6, 7 to 18, AreaAnswers.haa("GTDHH11"))), "13 days")
        assertEquals(listOf("GTDCL", "GTDHH11"), codes(leave("GTDCL", 7 to 6, 7 to 19, AreaAnswers.haa("GTDHH11"))), "14 days")
    }

    @Test
    fun aCasualLeaveOf14DaysDraftsNoFieldRegrantAndOneOf15Does() {
        // why: p.55 note 2 - "except when he rejoins from Casual Leave or hospitalization of less than 15 days".
        assertEquals(listOf("GTDCL"), codes(leave("GTDCL", 7 to 6, 7 to 19, AreaAnswers.field("GTDCFAA"))), "14 days")
        assertEquals(listOf("GTDCL", "GTDCFAA"), codes(leave("GTDCL", 7 to 6, 7 to 20, AreaAnswers.field("GTDCFAA"))), "15 days")
    }

    @Test
    fun sccAlwaysRegrantsEvenAfterAOneDayCasualLeaveAndAfterASpecialCasualLeave() {
        // why: p.53 note 2 names leave, hospitalization and courses and no exception. Special casual leave is not casual leave for the HAA and field exceptions.
        assertEquals(listOf("GTDCL", "GTDCIAFD"), codes(leave("GTDCL", 7 to 6, 7 to 6, AreaAnswers.sccia("GTDCIAFD"))))
        assertEquals(listOf("GTDSPCL", "GTDHH11", "GTDCFAA"), codes(leave("GTDSPCL", 7 to 6, 7 to 8, AreaAnswers.haa("GTDHH11"), AreaAnswers.field("GTDCFAA"), extra = mapOf("reason" to RealPart2.text("sport")))))
    }

    @Test
    fun theSccRowPrintsTheOperationInColumns3And8AndTheCorpsNotificationInColumns6And7() {
        val grant = al(AreaAnswers.sccia("GTDCIAMF")).last()
        assertEquals("GTDCIAMF", grant.code)
        assertEquals(
            "Granted Special Compensatory Counter Insurgency Allowance from 05/08/2026 on entering CI Ops area OP TESTONE in modified field as notified vide Bravo Corps notification No N-21/2026 date 09/04/2026 at Srl No 7",
            grant.narrative,
        )
        assertEquals(listOf("Bravo, N-21/2026, 09/04/2026", "7", "OP TESTONE", ""), grant.data, "Data 4 is not required")
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 53, "2"), grant.origin)
        val certificate = grant.certificate.orEmpty()
        assertTrue("CERTIFICATE : SCCIA" in certificate && "eligible to draw SCCIA" in certificate, certificate)
        assertTrue("included at Sr. No. 7 of Bravo Corps Notification No. N-21/2026 dated 09/04/2026" in certificate, certificate)
        assertTrue("on entering CI Ops OP TESTONE in peace, as notified vide Bravo Corps" in al(AreaAnswers.sccia("GTDCIAPC")).last().narrative)
        assertTrue("on entering C.I. Ops area OP TESTONE in field as notified vide Bravo Corps" in al(AreaAnswers.sccia("GTDCIAFD")).last().narrative)
    }

    @Test
    fun theFieldRowsPrintTheLocationNotificationAndSerialAndOnlyTheHighlyActiveOneHasAToDate() {
        val cfaa = al(AreaAnswers.field("GTDCFAA")).last()
        assertEquals("Granted Compensatory Field Area Allowance from 05/08/2026 on entering Field Area at location Testpur as notified vide Charlie Corps notification No N-33/2026 date 11/05/2026 at Srl No 41", cfaa.narrative)
        assertEquals(listOf("Charlie, N-33/2026, 11/05/2026", "41", "Testpur", ""), cfaa.data)
        assertNull(cfaa.to, "the allowance has no sanction period")
        assertEquals(Part2Origin.Linked(LinkBasis.MANUAL, 55, "2"), cfaa.origin)
        assertTrue("Modified Field Area Allowance from 05/08/2026 on entering Modified Field Area at location Testpur" in al(AreaAnswers.field("GTDCMFA")).last().narrative)
        val hafa = al(AreaAnswers.field("GTDHAFA")).last()
        assertEquals(day(12, 31), hafa.to, "note 3: the period till the Government sanction goes in To")
        assertEquals("Granted Highly Active Field Area Allowance from 05/08/2026 on entering Highly Active Field Area at location Testpur as notified vide Charlie Corps notification No N-33/2026 date 11/05/2026 at Srl No 41", hafa.narrative)
        val certificate = cfaa.certificate.orEmpty()
        assertTrue("CERTIFICATE : HAFA/CFAA/CMFA" in certificate && "stationed at Testpur in Field Area on duty w.e.f. 05/08/2026" in certificate, certificate)
        assertTrue("Sr. No. 41 of Charlie Corps Notification No. N-33/2026 dated 11/05/2026" in certificate, certificate)
    }

    @Test
    fun theOperationOfAHighlyActiveFieldAreaGrantIsOptionalInColumn9() {
        // why: note 5 (p.55): "Name of the Operation be notified in Col No 9, if applicable".
        assertEquals("", al(AreaAnswers.field("GTDHAFA")).last().data[3])
        val withOperation = rows(RealPart2.draft(OccurrenceRequest("GTDAL", annualLeave + AreaAnswers.everything + ("field_operation" to RealPart2.text("OP TESTTWO")), setOf(AreaAnswers.field("GTDHAFA")))))
        assertEquals("OP TESTTWO", withOperation.last().data[3])
    }

    @Test
    fun noStopIsDraftedForHaaSccAOrTheFieldAreasWhateverTheOfficerAnswers() {
        // why: Round 9 - re-grant only until there is evidence of what HRMS publishes; the stop codes are STPHH, STPSCCIA and STPFIELD.
        val answers = AreaRules.haaCodes.map(AreaAnswers::haa) + AreaRules.scciaCodes.map(AreaAnswers::sccia) + AreaRules.fieldCodes.map(AreaAnswers::field)
        for (answer in answers) {
            val rows = al(answer).map { it.code }
            assertEquals(2, rows.size, "$answer: the leave and the re-grant, nothing else: $rows")
            assertFalse(rows.any { it in setOf("STPHH", "STPSCCIA", "STPFIELD") }, "$answer: $rows")
        }
    }

    @Test
    fun anAnnualLeaveInAFieldAreaWithRhAndRationIsOneOrderInDateOrder() {
        // why: Round 9 and para 6.4 - the ration four, the R&H stop and re-grant and the area re-grant are one order, in date order, from the real bundle.
        val request = OccurrenceRequest("GTDAL", annualLeave + AreaAnswers.everything, setOf(RationRules.ANSWER_ON_RATION, AreaRules.ANSWER_RH_PREFIX + "GTDRH12", AreaAnswers.field("GTDCFAA")))
        val slip = RealPart2.draft(request)
        val order = slip.orders.single()
        assertEquals(
            listOf("SORS" to day(7, 4), "GTDRSHNA" to day(7, 4), "STPRH" to day(7, 4), "GTDAL" to day(7, 6), "TORS" to day(8, 5), "STPRSHNA" to day(8, 5), "GTDRH12" to day(8, 5), "GTDCFAA" to day(8, 5)),
            order.occurrences.map { it.code to it.from },
        )
        assertEquals(emptyList(), slip.findings)
        assertEquals(emptyList(), slip.undrafted)
    }
}
