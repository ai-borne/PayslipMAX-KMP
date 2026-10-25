package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RealPart2.draft
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.rows
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden slips for content batch 2, CANCEL and CANCELS (Part II plan, phase 6c; ODP-2026 pp.226-227), on the real shipped templates and
 * synthetic orders only. They pin what the owner reviews against the printed pages: "ALL" in col 6, the order in cols 8 and 9, the code in
 * col 6 and the serial in col 7 of CANCELS, the certificate, and when the time-bar and quoting notes fire.
 */
class Part2CancelGoldenTest {
    private val order = ref("0123/26", 6, 20)
    private val cancelAll = OccurrenceRequest("CANCEL", mapOf("cancelled" to order))

    private fun cancelOne(serial: Int = 3) = OccurrenceRequest("CANCELS", mapOf("cancelled" to order, "cancelled_code" to text("GTDAL"), "cancelled_serial" to number(serial)))

    private fun findings(
        request: OccurrenceRequest,
        today: CalendarDate?,
    ) = (RealPart2.engine.draft(LeaveEventInput(listOf(request)), today) as DraftResult.Drafted).slip.findings

    @Test
    fun cancelPrintsAllInCol6TheOrderInCols8And9AndTheCertificateAndNoDates() {
        val row = rows(draft(cancelAll)).single()

        assertEquals("CANCEL", row.code)
        assertEquals("A", row.group)
        assertEquals("Cancelled all occurrence of Part II Order Number 0123/26 dated 20/06/2026", row.narrative)
        assertEquals(null to null, row.from to row.to, "cols 4 and 5 are not required")
        assertEquals(listOf("ALL", "", "0123/26", "20/06/2026"), row.data, "col 6 ALL, col 7 not required, cols 8 and 9 the order")
        assertEquals("Certified that the previous status may be restored due to cancellation of Part II Order No 0123/26 dated 20/06/2026", row.certificate)
        assertNull(row.authority)
        assertEquals(Part2Origin.Requested, row.origin)
    }

    @Test
    fun cancelsPrintsTheCodeInCol6TheSerialInCol7AndTheOrderInCols8And9() {
        val row = rows(draft(cancelOne(3))).single()

        assertEquals("CANCELS", row.code)
        assertEquals("Cancelled occurrence at Ser No 3 of Pt II order No 0123/26 dated 20/06/2026", row.narrative)
        assertEquals(listOf("GTDAL", "3", "0123/26", "20/06/2026"), row.data)
        assertEquals("Certified that the previous status may be restored due to cancellation of Sr No 3 of the Part II Order No 0123/26 dated 20/06/2026", row.certificate)
        assertEquals(null to null, row.from to row.to)
    }

    @Test
    fun twoOccurrencesToCancelAreTwoRowsBecauseOneCancelsCoversOneOccurrenceOnly() {
        // why: p.227 note 7 - CANCELS is used for one occurrence only, so two wrong rows of one order need two CANCELS rows.
        val slip = draft(cancelOne(3), cancelOne(5))

        assertEquals(listOf("3", "5"), rows(slip).map { it.data[1] })
        assertEquals(1, slip.orders.size)
    }

    @Test
    fun aCancellationWithNoOrderYetStillDraftsAndPrintsBlanksForTheClerkAndWarns() {
        val slip = draft(OccurrenceRequest("CANCEL"))
        val row = rows(slip).single()

        assertEquals("Cancelled all occurrence of Part II Order Number ____ dated ____", row.narrative)
        assertEquals(listOf("ALL", "", "____", "____"), row.data)
        assertEquals(listOf("cancel_quoted"), slip.findings.map { it.ruleId })
        assertEquals(RuleSeverity.WARN, slip.findings.single().severity)
        assertEquals(226 to "2", slip.findings.single().page to slip.findings.single().note)
    }

    @Test
    fun cancelsNeedsTheCodeAndTheSerialAndNamesWhichIsMissing() {
        val result = RealPart2.engine.draft(LeaveEventInput(listOf(OccurrenceRequest("CANCELS", mapOf("cancelled" to order)))))

        val refused = result as DraftResult.Refused
        assertEquals(setOf("cancelled_code", "cancelled_serial"), refused.issues.map { it.slot }.toSet())
        assertTrue(refused.issues.all { it.reason == DraftIssueReason.NOT_ANSWERED })
    }

    @Test
    fun anOrderOverTwoYearsOldRaisesTheTimeBarWarningOnBothCodesWithTheirOwnCitation() {
        val today = CalendarDate(2026, 10, 25)
        val old = ref("0050/24", 10, 24, 2024)

        val all = findings(OccurrenceRequest("CANCEL", mapOf("cancelled" to old)), today).single()
        val one = findings(OccurrenceRequest("CANCELS", cancelOne().values + ("cancelled" to old)), today).single()

        assertEquals(Triple("cancel_time_barred", RuleSeverity.WARN, "CANCEL"), Triple(all.ruleId, all.severity, all.code))
        assertEquals(226 to "4", all.page to all.note)
        assertEquals(Triple("cancels_time_barred", RuleSeverity.WARN, "CANCELS"), Triple(one.ruleId, one.severity, one.code))
        assertEquals(227 to "6", one.page to one.note)
    }

    @Test
    fun aYoungerOrderAndASlipDraftedWithoutTheDateRaiseNoTimeBarWarning() {
        assertEquals(emptyList(), findings(cancelAll, CalendarDate(2026, 10, 25)), "a 2026 order")
        assertEquals(emptyList(), findings(OccurrenceRequest("CANCEL", mapOf("cancelled" to ref("0050/24", 1, 1, 2020))), today = null), "no device date, no judgment")
    }
}
