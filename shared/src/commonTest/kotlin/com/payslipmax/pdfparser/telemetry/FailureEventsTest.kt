package com.payslipmax.pdfparser.telemetry

import com.payslipmax.pdfparser.testing.FakeCrashReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, "From P10" item 5): Android's reporter forwards a recorded exception's message to Crashlytics. A PDF library's
 * message can quote the file or a row, and an installer's message is its own text. So a failed import or install records an event of a
 * fixed message, the exception's type, and one of our own fixed codes; the original text never leaves, whatever it said.
 */
class FailureEventsTest {
    private val leaky =
        listOf(
            "Cannot open /data/user/0/in.aiborne.payslipmax/cache/AB123 Payslip Sunil.pdf",
            "Row for ABCDE1234F, net pay 98765.43 not found",
            "UNRECOGNIZED_GRAMMAR near ABCDE1234F 98765.43",
        )

    @Test
    fun aFailedImportRecordsAFixedEventTheTypeAndOneOfOurCodesAndNothingTheExceptionSaid() {
        for (text in leaky) {
            val reporter = FakeCrashReporter()

            reporter.recordPdfImportFailure(IllegalStateException(text), fileSizeBytes = 2048)

            val (event, keys) = reporter.exceptions.single()
            assertIs<PdfImportFailureEvent>(event)
            assertEquals("pdf_import_failed", event.message)
            val everything = (listOf(event.toString()) + keys.orEmpty().keys + keys.orEmpty().values).joinToString("|")
            for (secret in listOf("ABCDE1234F", "98765", "Sunil", "/data/user", "Row for")) {
                assertFalse(secret in everything, "'$secret' must not leave the device (from: $text)")
            }
            assertEquals("pdf_import", keys?.get("operation"))
            assertEquals("IllegalStateException", keys?.get("error_type"))
            assertEquals("2048", keys?.get("file_size_bytes"))
        }
    }

    @Test
    fun theReasonIsOneOfTheParsersFixedCodesAndAnythingElseIsOther() {
        assertEquals(ImportFailureReason.UNRECOGNIZED_GRAMMAR, ImportFailureReason.of("UNRECOGNIZED_GRAMMAR near ABCDE1234F"))
        assertEquals(ImportFailureReason.PREFLIGHT, ImportFailureReason.of("PdfPreFlightValidationFailed: /x/y.pdf"))
        assertEquals(ImportFailureReason.NO_TEXT_TOKENS, ImportFailureReason.of("NO_TEXT_TOKENS"))
        assertEquals(ImportFailureReason.OTHER, ImportFailureReason.of("Row for ABCDE1234F"))
        assertEquals(ImportFailureReason.OTHER, ImportFailureReason.of(null))
        val reporter = FakeCrashReporter()
        reporter.recordPdfImportFailure(IllegalStateException(leaky[2]), 1)
        assertEquals(ImportFailureReason.UNRECOGNIZED_GRAMMAR, reporter.exceptions.single().second?.get("error_reason"))
        assertEquals("preflight", reporter.exceptions.single().second?.get("parser_stage"))
    }

    @Test
    fun aGrammarFailureIsReportedAsThePreflightStageAndOthersAsGrammar() {
        val reporter = FakeCrashReporter()
        reporter.recordPdfImportFailure(IllegalStateException("UNRECOGNIZED_GRAMMAR"), 1)
        reporter.recordPdfImportFailure(IllegalStateException("something else"), 1)

        assertEquals(listOf("preflight", "grammar"), reporter.exceptions.map { it.second?.get("parser_stage") })
    }

    @Test
    fun aFailedGemmaInstallRecordsAFixedEventAndCarriesNothingTheInstallerSaid() {
        val reporter = FakeCrashReporter()

        reporter.recordGemmaInstallFailure()

        val (event, keys) = reporter.exceptions.single()
        assertIs<GemmaInstallFailureEvent>(event)
        assertEquals("gemma_install_failed", event.message)
        assertTrue(keys.orEmpty().isEmpty(), "no installer text goes along as a key")
        assertFalse("Disk full" in event.toString() || "ABCDE" in event.toString())
    }
}
