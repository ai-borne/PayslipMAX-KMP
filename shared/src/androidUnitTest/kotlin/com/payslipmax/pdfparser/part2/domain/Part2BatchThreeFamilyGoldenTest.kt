package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.Ch5Answers.leave
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.on
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.order
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.request
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.rowOf
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Golden slips for the maternity, child-care, adoption, paternity and late-back codes of content batch 3 (Part II plan, phase 8; ODP-2026
 * pp.117, 122-124, 129-130), on the real shipped templates and synthetic dates and orders only.
 */
class Part2BatchThreeFamilyGoldenTest {
    private val first = "occasion" to text("First")

    @Test
    fun maternityLeavePrintsTheOccasionInData4AndNilForAnAbsentPrefixAndSuffix() {
        val row = rowOf("MTRNLV", request("MTRNLV", leave(on(11, 1), on(4, 29, 2027), first)))

        assertEquals("Granted 180 days Maternity Leave for First child from 01/11/2026 to 29/04/2027 with permission to prefix Nil and suffix Nil", row.narrative)
        assertEquals(listOf("", "", "180", "First"), row.data, "p.117: Data 1 prefix, Data 2 suffix, Data 3 days, Data 4 the occasion")
    }

    @Test
    fun specialMaternityLeavePrintsTheSpellAndTheReasonAndKeepsTheManualsFullStop() {
        val values = leave(on(9, 1), on(9, 30), "spell" to text("First"), "reason" to text("Miscarriage"))
        val row = rowOf("SPMTRNLV", request("SPMTRNLV", values))

        assertEquals("Granted 30 days Special Maternity Leave, First spell for Miscarriage from 01/09/2026 to 30/09/2026 with permission to prefix Nil and suffix Nil.", row.narrative)
        assertEquals(listOf("", "", "30", "First"), row.data)
    }

    @Test
    fun childCareLeavePrintsDaysAndSpellInData3TheYearTotalInData4AndTheManualsCertificate() {
        val values = leave(on(9, 1), on(9, 10), "spell" to text("1st"), "ccl_year_days" to number(10))
        val row = rowOf("GTDCCL", request("GTDCCL", values))

        assertEquals(
            "Granted Child Care Leave for 10 days, 1st spell from 01/09/2026 to 10/09/2026 with permission to prefix on Nil & suffix on Nil. " +
                "Total number of days of CCL availed during the current calendar year, including the CCL granted now, is 10 days.",
            row.narrative,
        )
        assertEquals(listOf("", "", "10/1st", "10"), row.data, "p.117: Data 3 days of CCL and spell, Data 4 the total this year")
        assertEquals(true, row.certificate.orEmpty().startsWith("CERTIFICATE FOR GRANT OF CHILD CARE LEAVE Certified that: 1. The date of birth of the child is"), "p.121")
        assertEquals(true, row.certificate.orEmpty().endsWith("Male Officer and entitled to avail CCL."))
    }

    @Test
    fun childAdoptionLeavePrintsAFixed180AndTheRuleAndLetterOfTheManualAsAuthority() {
        val row = rowOf("CHADPLVE", request("CHADPLVE", leave(on(9, 1), on(2, 27, 2027), first)))

        assertEquals("Granted 180 days Child Adoption Leave for First child from 01/09/2026 to 27/02/2027 with permission to prefix Nil and suffix Nil", row.narrative)
        assertEquals(listOf("", "", "180", "First"), row.data, "p.122")
        assertEquals("Rule 56-B Leave Rules for the Services-Vol.-I Army and GoI, MoD letter No B/33922/AG/PS- 2(b)/5/US (AG-II)/D(AG) dated 15 Jan 2016.", row.authority, "p.123 Auth.")
    }

    @Test
    fun paternityLeavePrintsTheDaysAndTheOccasion() {
        val row = rowOf("PTRNLV", request("PTRNLV", leave(on(9, 1), on(9, 15), first)))

        assertEquals("Granted Paternity Leave from 01/09/2026 to 15/09/2026 for 15 days with permission to prefix Nil And suffix Nil", row.narrative)
        assertEquals(listOf("", "", "15", "First"), row.data, "p.123")
    }

    @Test
    fun paternityLeaveOnAdoptionPrintsAFixed15AndTheOccasionWithTheAdoptionOrderInData4() {
        val row = rowOf("PATLCHAD", request("PATLCHAD", leave(on(9, 1), on(9, 15), first, "adoption" to ref("0789/26", 7, 3))))

        assertEquals("Granted Paternity Leave on Child Adoption from 01/09/2026 to 15/09/2026 for 15 days with permission to prefix and suffix. Part II order for adoption pub vide PTO No 0789/26 & dt 03/07/2026", row.narrative)
        assertEquals(listOf("", "", "15", "First, 0789/26, 03/07/2026"), row.data, "p.124: Data 4 the occasion, then the adoption order's number and date")
    }

    @Test
    fun overstayedLeaveQuotesTheGrantOrderInData3And4AndHasNoToDate() {
        val values = mapOf("leave_type" to text("annual leave"), "absent_from" to RealPart2.date(10, 1), "grant" to order)
        val row = rowOf("OSL", request("OSL", values))

        assertEquals("Overstayed annual leave from 01/10/2026", row.narrative)
        assertEquals(on(10, 1) to null, row.from to row.to)
        assertEquals(listOf("annual leave", "", "0123/26", "20/06/2026"), row.data, "p.129 and p.130 note 2: Data 1 the kind, Data 3 and 4 the order no. and date")
    }

    @Test
    fun rejoiningFromOverstayalQuotesTheOrderThatDeclaredItAndKeepsTheFullStop() {
        val values = mapOf("leave_type" to text("annual leave"), "absent_from" to RealPart2.date(10, 1), "absent_to" to RealPart2.date(10, 5), "declared" to ref("0888/26", 10, 6))
        val row = rowOf("REJOSL", request("REJOSL", values))

        assertEquals("Rejoined from Overstayal of annual leave from 01/10/2026 to 05/10/2026.", row.narrative)
        assertEquals(on(10, 1) to on(10, 5), row.from to row.to)
        assertEquals(listOf("annual leave", "", "0888/26", "06/10/2026"), row.data, "p.130 note 3")
    }

    @Test
    fun absenceWithoutLeaveHasOnlyAFromDate() {
        val row = rowOf("AWL", request("AWL", mapOf("absent_from" to RealPart2.date(10, 1))))

        assertEquals("Absent without leave from 01/10/2026", row.narrative)
        assertEquals(on(10, 1) to null, row.from to row.to)
        assertEquals(listOf("", "", "", ""), row.data, "p.130: every Data column is not required")
        assertNull(row.authority)
    }

    @Test
    fun rejoiningFromAbsenceQuotesTheOrderThatDeclaredIt() {
        val values = mapOf("absent_from" to RealPart2.date(10, 1), "absent_to" to RealPart2.date(10, 5), "declared" to ref("0888/26", 10, 6))
        val row = rowOf("REJAWL", request("REJAWL", values))

        assertEquals("Rejoined from Absent without leave from 01/10/2026 to 05/10/2026.", row.narrative)
        assertEquals(listOf("", "", "0888/26", "06/10/2026"), row.data, "p.130 note 1")
    }
}
