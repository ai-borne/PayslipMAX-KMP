package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The leave balance drives a retirement-encashment figure worth lakhs, so a wrong or partial table is worse than no table:
 * every ambiguous section must come back null, and a parsed one must keep PCDA's printed figures exactly.
 * Synthetic text only; the real layouts are covered by LeaveAccumulationCorpusTest.
 */
class LeaveAccumulationExtractorTest {
    private val modernSection =
        """
        Principal Controller of Defence Accounts (Officers),Pune
        Statement of account for 12/2025
        LEAVE ACCUMULATION DETAILS
        Cas Date Accu AL Total Accu AL Cas code Do2 No Do2 Dt
        31/12/2016 24 110 ACCLVE 13 17/04/2017
        26/04/2017 10 LVELTC 65 06/05/2018
        31/12/2017 130 130 ACCLVE 66 06/05/2018
        01/12/2020 0 10 LVELTC 84 04/02/2021
        31/12/2024 19 301 ACCLVE 381 02/05/2025
        1. As per GoI, MoD letter No.B/33931/AG/PS-2(b)/1908/D(AG) dated 12-04-2010, Quantum of leave encashment
        """.trimIndent()

    private val section2022 =
        """
        Leave Accumulation Details
        SrNo Cas Date Accu AL Tot Accu AL Cas Code Do2 No Do2 Dt Leave
        1 31.12.2008 07 07 ACCLVE 00022 10.03.2009 0
        10 04.03.2018 10 LVELTC 00205 06.12.2018 10
        12 31.12.2019 30 180 ACCLVE 0446/2021 06.05.2021 0
        Above leave data available with PCDA(O) till Dec 2022, for accumulation of leave and encashment against LTC.
        """.trimIndent()

    @Test
    fun modernLayoutKeepsPrintedFiguresAndConvertsDatesToIso() {
        val entries = assertNotNull(LeaveAccumulationExtractor.extract(modernSection)).entries

        assertEquals(5, entries.size)
        assertEquals(LeaveEntry("2016-12-31", LeaveCasCode.ACCRUED, 24, 110, "13", "2017-04-17"), entries[0])
        // PCDA prints 130/130 here although 110 + 30 - 10 = 130 would be the arithmetic; the printed figure is the record.
        assertEquals(LeaveEntry("2017-12-31", LeaveCasCode.ACCRUED, 130, 130, "66", "2018-05-06"), entries[2])
        assertEquals(LeaveEntry("2024-12-31", LeaveCasCode.ACCRUED, 19, 301, "381", "2025-05-02"), entries[4])
    }

    @Test
    fun ltcRowsCarryDaysEncashedAndNoRunningTotal() {
        val entries = assertNotNull(LeaveAccumulationExtractor.extract(modernSection)).entries

        // Accu AL left blank on the page: the single number is the days encashed, never a balance.
        assertEquals(LeaveEntry("2017-04-26", LeaveCasCode.LTC_ENCASHED, 10, null, "65", "2018-05-06"), entries[1])
        // Accu AL printed as 0: still 10 days encashed, not a balance of 10.
        assertEquals(LeaveEntry("2020-12-01", LeaveCasCode.LTC_ENCASHED, 10, null, "84", "2021-02-04"), entries[3])
    }

    @Test
    fun layout2022WithSerialNumbersDottedDatesAndTrailingColumnParses() {
        val entries = assertNotNull(LeaveAccumulationExtractor.extract(section2022)).entries

        assertEquals(3, entries.size)
        assertEquals(LeaveEntry("2008-12-31", LeaveCasCode.ACCRUED, 7, 7, "00022", "2009-03-10"), entries[0])
        assertEquals(LeaveEntry("2018-03-04", LeaveCasCode.LTC_ENCASHED, 10, null, "00205", "2018-12-06"), entries[1])
        assertEquals(LeaveEntry("2019-12-31", LeaveCasCode.ACCRUED, 30, 180, "0446/2021", "2021-05-06"), entries[2])
    }

    @Test
    fun crlfLineEndingsAndExtraSpacesParseTheSame() {
        val noisy = modernSection.replace("\n", "\r\n").replace(" ACCLVE ", "   ACCLVE  ")

        assertEquals(LeaveAccumulationExtractor.extract(modernSection), LeaveAccumulationExtractor.extract(noisy))
    }

    @Test
    fun unknownCasCodeIsKeptAsPrintedRatherThanDropped() {
        val text = modernSection.replace("26/04/2017 10 LVELTC", "26/04/2017 10 LVECOMP")

        val entry = assertNotNull(LeaveAccumulationExtractor.extract(text)).entries[1]

        assertEquals("LVECOMP", entry.casCode)
        assertEquals(10, entry.days)
        assertNull(entry.totalDays)
    }

    @Test
    fun payslipWithoutTheLeavePageGivesNull() {
        val text = "STATEMENT OF ACCOUNT FOR 07/2025\nBPAY (12A) 144700\nDA 92916\nDSOP 40000"

        assertNull(LeaveAccumulationExtractor.extract(text))
    }

    @Test
    fun headingWithoutTheColumnHeaderGivesNull() {
        val text = modernSection.replace("Cas Date Accu AL Total Accu AL Cas code Do2 No Do2 Dt", "Some other table")

        assertNull(LeaveAccumulationExtractor.extract(text))
    }

    @Test
    fun headerWithNoRowsGivesNull() {
        val text = "LEAVE ACCUMULATION DETAILS\nCas Date Accu AL Total Accu AL Cas code Do2 No Do2 Dt\n1. As per GoI"

        assertNull(LeaveAccumulationExtractor.extract(text))
    }

    @Test
    fun accruedRowMissingItsRunningTotalFailsTheWholeSection() {
        val text = modernSection.replace("31/12/2024 19 301 ACCLVE", "31/12/2024 19 ACCLVE")

        assertNull(LeaveAccumulationExtractor.extract(text))
    }

    @Test
    fun impossibleCalendarDateFailsTheWholeSection() {
        assertNull(LeaveAccumulationExtractor.extract(modernSection.replace("31/12/2016", "31/02/2016")))
        assertNull(LeaveAccumulationExtractor.extract(modernSection.replace("17/04/2017", "17/13/2017")))
    }

    @Test
    fun leapDayIsAValidDate() {
        val text = modernSection.replace("26/04/2017 10 LVELTC 65 06/05/2018", "29/02/2016 10 LVELTC 65 06/05/2018")

        assertEquals("2016-02-29", assertNotNull(LeaveAccumulationExtractor.extract(text)).entries[1].casDate)
    }

    @Test
    fun overlongNumberFailsTheWholeSection() {
        assertNull(LeaveAccumulationExtractor.extract(modernSection.replace("19 301 ACCLVE", "19 30100 ACCLVE")))
    }

    @Test
    fun leaveRowsAfterTheBlockEndsMeanATruncatedTableAndGiveNull() {
        // A table split across pages would otherwise yield an early, wrong balance.
        val split = modernSection.replace("31/12/2024 19 301 ACCLVE", "Page 6 of 8\n31/12/2024 19 301 ACCLVE")

        assertNull(LeaveAccumulationExtractor.extract(split))
    }

    @Test
    fun tableLongerThanTheRowCapGivesNull() {
        val header = "LEAVE ACCUMULATION DETAILS\nCas Date Accu AL Total Accu AL Cas code Do2 No Do2 Dt\n"
        val rows = (1..LeaveAccumulationExtractor.MAX_ROWS + 1).joinToString("\n") { "31/12/2010 01 $it ACCLVE 1 01/01/2011" }

        assertNull(LeaveAccumulationExtractor.extract(header + rows))
    }

    @Test
    fun hugeTextWithoutNewlinesGivesNull() {
        assertNull(LeaveAccumulationExtractor.extract("LEAVE ACCUMULATION DETAILS " + "31/12/2010 01 ".repeat(50_000)))
    }
}
