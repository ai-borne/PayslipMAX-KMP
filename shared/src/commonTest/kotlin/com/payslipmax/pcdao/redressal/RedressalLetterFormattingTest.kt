package com.payslipmax.pcdao.redressal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedressalLetterFormattingTest {
    @Test
    fun testMathematicalDiscrepancyTableFormatting() {
        val items =
            listOf(
                DiffLineItem(
                    serialNo = 1,
                    lineItemName = "Children Education Allowance (CEA)",
                    entitledAmount = 67500.0,
                    creditedAmount = 54000.0,
                    netDue = 13500.0,
                    statutoryAuthority = "DoPT OM No. A-27012/02/2017-Estt.(AL) dated 16/17 July 2018; Para 4",
                ),
                DiffLineItem(
                    serialNo = 2,
                    lineItemName = "Siachen Allowance",
                    entitledAmount = 53125.0,
                    creditedAmount = 42500.0,
                    netDue = 10625.0,
                    statutoryAuthority = "MoD Letter No. 1(16)/2017/D(Pay/Services) dated 18 Sep 2017",
                ),
            )

        val request =
            RedressalRequest(
                officerName = "Amit Sharma",
                serviceNumber = "IC-61234M",
                rank = "Major",
                cdaAccountNo = "08/112/994433",
                ledgerSection = "Section R (Regimental Officers)",
                disputeMonth = "July 2026",
                lineItems = items,
            )

        val letter = RedressalLetterGenerator.generateLetter(request)

        assertTrue(letter.discrepancyTableText.contains("ENTITLED"))
        assertTrue(letter.discrepancyTableText.contains("CREDITED"))
        assertTrue(letter.discrepancyTableText.contains("NET DUE"))
        assertTrue(letter.discrepancyTableText.contains("Rs. 67500"))
        assertTrue(letter.discrepancyTableText.contains("Rs. 54000"))
        assertTrue(letter.discrepancyTableText.contains("Rs. 13500"))
        assertTrue(letter.discrepancyTableText.contains("TOTAL STATUTORY NET DUE: Rs. 24125"))
        assertEquals(24125.0, letter.totalNetDue)

        // Assert table columns and values format cleanly across standard mobile widths (<= 72 chars/line)
        val lines = letter.discrepancyTableText.lines()
        lines.forEach { line ->
            assertTrue(line.length <= 72, "Table line exceeds mobile-friendly width: '$line' (${line.length})")
        }
    }

    @Test
    fun testStatutoryCitationsFormatting() {
        val items =
            listOf(
                DiffLineItem(
                    serialNo = 1,
                    lineItemName = "Transport Allowance (TPTA)",
                    entitledAmount = 10800.0,
                    creditedAmount = 5400.0,
                    netDue = 5400.0,
                    statutoryAuthority = "MoD Letter No. 1(26)/1997/D(Pay/Services) dated 29.02.2000 & 7th CPC Para 8.15.53",
                ),
            )
        val citations = RedressalLetterGenerator.formatStatutoryCitations(items, ExportFormat.TXT)
        assertTrue(citations.contains("(a) Transport Allowance (TPTA):"))
        assertTrue(citations.contains("MoD Letter No. 1(26)/1997/D(Pay/Services)"))

        val mdCitations = RedressalLetterGenerator.formatStatutoryCitations(items, ExportFormat.MARKDOWN)
        assertTrue(mdCitations.contains("- **Transport Allowance (TPTA)**:"))
    }

    @Test
    fun testMarkdownExportFormat() {
        val items =
            listOf(
                DiffLineItem(
                    serialNo = 1,
                    lineItemName = "Hostel Subsidy",
                    entitledAmount = 101250.0,
                    creditedAmount = 81000.0,
                    netDue = 20250.0,
                    statutoryAuthority = "DoPT OM No. A-27012/02/2017-Estt.(AL)",
                ),
            )
        val table = RedressalLetterGenerator.formatDiscrepancyTable(items, ExportFormat.MARKDOWN)
        assertTrue(table.contains("| SR. | DISCREPANCY LINE ITEM | ENTITLED | CREDITED | NET DUE |"))
        assertTrue(table.contains("| 1 | Hostel Subsidy | Rs. 101250 | Rs. 81000 | Rs. 20250 |"))
        assertTrue(table.contains("**TOTAL STATUTORY NET DUE**: Rs. 20250"))
    }
}
