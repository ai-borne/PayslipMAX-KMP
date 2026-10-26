package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.Ch5Answers.furlough
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.leave
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.letter
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.on
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.order
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.request
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.rowOf
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Golden slips for the furlough, terminal and study leave of content batch 3 (Part II plan, phase 8; ODP-2026 pp.116-119, 125-128), on
 * the real shipped templates and synthetic dates and orders only. They pin what the owner reviews against the printed pages: the
 * narrative, From and To, what each Data column carries and the authority line.
 */
class Part2BatchThreeGoldenTest {
    @Test
    fun furloughPrintsTheCycleInTheNarrativeAndInData4AndTheDayCountInData3() {
        val row = rowOf("FURLEV", request("FURLEV", furlough))

        assertEquals("Granted 30 days Furlough Leave for the cycle 2024-2026 from 06/07/2026 to 04/08/2026 with permission to prefix 04/07/2026 & suffix Nil", row.narrative)
        assertEquals(on(7, 6) to on(8, 4), row.from to row.to)
        assertEquals(listOf("04/07/2026", "", "30", "2024-2026"), row.data, "p.116: Data 1 prefix, Data 2 suffix (empty when none), Data 3 days, Data 4 cycle")
        assertEquals("A", row.group)
    }

    @Test
    fun anExtensionOfFurloughQuotesTheGrantOrderInTheNarrativeAndData1AndTheCycleInData4() {
        val values = leave(on(8, 5), on(8, 14), "suffix_last" to date(8, 16), "cycle_from" to number(2024), "cycle_to" to number(2026), "grant" to order)
        val row = rowOf("EXTNFUR", request("EXTNFUR", values))

        assertEquals(
            "Granted 10 days extension of Furlough Leave from 05/08/2026 to 14/08/2026 with permission to suffix 16/08/2026. " +
                "Furlough leave initially granted vide Part II Order No 0123/26 and date 20/06/2026 for that cycle of calendar years 2024-2026",
            row.narrative,
        )
        assertEquals(listOf("0123/26 dated 20/06/2026", "16/08/2026", "10", "2024-2026"), row.data, "p.128: Data 1 the grant order, Data 2 suffix, Data 3 days, Data 4 cycle")
    }

    @Test
    fun anExtensionWithNoGrantOrderYetPrintsBlanksForTheClerk() {
        val values = leave(on(8, 5), on(8, 14), "cycle_from" to number(2024), "cycle_to" to number(2026))
        val row = rowOf("EXTNFUR", request("EXTNFUR", values))

        assertEquals("____ dated ____", row.data[0])
        assertEquals("Nil", row.narrative.substringAfter("suffix ").substringBefore("."))
    }

    @Test
    fun extraordinaryLeavePrintsOnlyTheDaysInData3() {
        val row = rowOf("EOL", request("EOL", leave(on(9, 1), on(9, 30))))

        assertEquals("Granted 30 days EOL without pay and allowances from 01/09/2026 to 30/09/2026", row.narrative)
        assertEquals(listOf("", "", "30", ""), row.data, "p.118: Data 1, 2 and 4 are not required")
    }

    @Test
    fun terminalLeavePrintsAFixed28InTheNarrativeAndData3WhateverTheDatesSpan() {
        val alone = rowOf("TERLEV", request("TERLEV", leave(on(10, 1), on(10, 28))))
        assertEquals("Granted 28 days Terminal Leave from 01/10/2026 to 28/10/2026", alone.narrative)
        assertEquals(listOf("", "", "28", ""), alone.data)

        val beside = rowOf("TERLEV", request("TERLEV", leave(on(10, 1), on(11, 27))))
        assertEquals("Granted 28 days Terminal Leave from 01/10/2026 to 27/11/2026", beside.narrative, "note 10: From is the annual leave's first day, To the day the terminal leave expires")
        assertEquals("28", beside.data[2], "the 28 is the manual's, not the span")
    }

    @Test
    fun studyLeavePrintsPlaceQualificationDaysAndTheServiceLiabilityWithTheLetterAsAuthority() {
        val values = leave(on(9, 1), on(2, 28, 2027), "qualification" to text("MBA"), "place" to text("Pune"), "liab_years" to number(2), "liab_months" to number(6), "letter" to letter)
        val row = rowOf("STDLEV", request("STDLEV", values))

        assertEquals("Granted 181 days Study Leave from 01/09/2026 to 28/02/2027 for acquiring the qualification MBA having mandatory service period of 2 years & 6 months.", row.narrative)
        assertEquals(listOf("Pune", "MBA", "181", "2 years 6 Months"), row.data, "p.125: Data 1 place, Data 2 qualification, Data 3 days, Data 4 liability")
        assertEquals("IHQ of MoD (Army)/DGAFMS letter No L-77/26 dated 02/05/2026", row.authority, "p.126 note 9")
    }

    @Test
    fun anExtensionOfStudyLeaveAgainstAnnualLeaveQuotesTheStudyLeaveOrderInData1AndTheYearInData4() {
        val values = leave(on(3, 1, 2027), on(3, 30, 2027), "year" to number(2027), "qualification" to text("MBA"), "grant" to ref("0456/26", 8, 1), "letter" to letter)
        val row = rowOf("STDLVAL", request("STDLVAL", values))

        assertEquals(
            "Granted extension of Study Leave for 30 days from 01/03/2027 to 30/03/2027 against Annual leave for 2027 for acquiring the qualification MBA. " +
                "Study leave granted vide Pt II order No. 0456/26 dated 01/08/2026",
            row.narrative,
        )
        assertEquals(listOf("0456/26 dated 01/08/2026", "MBA", "30", "2027"), row.data, "p.127 and p.128 note 7: Data 1 the order, Data 2 qualification, Data 3 days, Data 4 year")
        assertEquals("IHQ of MoD (Army)/DGAFMS letter No L-77/26 dated 02/05/2026", row.authority, "p.128 note 6")
    }

    @Test
    fun anExtensionOfStudyLeaveAgainstFurloughLeavesData2EmptyAndPrintsTheCycle() {
        val values = leave(on(3, 1, 2027), on(3, 30, 2027), "cycle_from" to number(2025), "cycle_to" to number(2027), "qualification" to text("MBA"), "grant" to ref("0456/26", 8, 1))
        val row = rowOf("STDLVFUR", request("STDLVFUR", values))

        assertEquals(
            "Granted extension of Study Leave for 30 days from 01/03/2027 to 30/03/2027 against Furlough leave for 2025-2027 for acquiring the qualification MBA. " +
                "Study leave granted vide Pt II order No. 0456/26 dated 01/08/2026",
            row.narrative,
        )
        assertEquals(listOf("0456/26 dated 01/08/2026", "", "30", "2025-2027"), row.data, "p.127: Data 2 is not required")
        assertEquals("IHQ of MoD (Army)/DGAFMS letter No ____ dated ____", row.authority, "no letter typed: blanks for the clerk")
        assertNull(row.certificate)
    }
}
