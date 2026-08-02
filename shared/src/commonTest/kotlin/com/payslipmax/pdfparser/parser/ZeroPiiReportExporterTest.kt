package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.payslipmax.pdfparser.parser.detection.GrammarFamily
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZeroPiiReportExporterTest {
    @Test
    fun testExportDiagnosticReport_scrubsAllPiiFields() {
        val report =
            GrammarDiagnosticReport(
                selectedFamily = GrammarFamily.PCDA_EXTENDED_GRID,
                selectedPriority = 1,
                isKnownGrammar = true,
                validationStatus = "PASSED",
                selectionReason = "Date mapping Mar 2025+",
            )

        val jsonOutput = ZeroPiiReportExporter.exportJson(report)

        assertTrue(jsonOutput.contains("PCDA_EXTENDED_GRID"))
        assertTrue(jsonOutput.contains("Officer"))
        assertTrue(jsonOutput.contains("000000X"))
        assertTrue(jsonOutput.contains("AR*****90G"))
        assertTrue(jsonOutput.contains("user@example.com"))
        assertTrue(jsonOutput.contains("PASSED"))
    }

    @Test
    fun testScrubRawPiiText() {
        val rawPii = "Officer Major General Vikram Singh CDA A/C 12/345/678901A PAN ABCDE1234F email vikram.singh@army.gov.in"
        val scrubbed = ZeroPiiReportExporter.scrubText(rawPii)

        assertFalse(scrubbed.contains("12/345/678901A"), "Must scrub account number")
        assertFalse(scrubbed.contains("ABCDE1234F"), "Must scrub PAN")
        assertFalse(scrubbed.contains("vikram.singh@army.gov.in"), "Must scrub email")
    }
}
