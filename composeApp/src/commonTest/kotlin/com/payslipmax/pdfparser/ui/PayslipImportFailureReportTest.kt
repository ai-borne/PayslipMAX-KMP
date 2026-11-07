package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipImportFailureReportTest {
    private val fakeParser = FakePdfParser()
    private val repository = PayslipRepository(FakePayslipDao(), fakeParser, Dispatchers.Unconfined)
    private val validPdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x35) // "%PDF-1.5"

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * WHY (docs/Plan/14a P10, "From P10" item 5): a failed import is reported to Crashlytics so a parser regression is seen, but the PDF
     * library's message can quote the file or a row and Android forwards it. The report carries the type, one of the parser's fixed codes
     * and the size; an encrypted PDF is not a failure to report at all.
     */
    @Test
    fun aFailedImportReportsTheTypeAndAFixedCodeNeverTheMessageAndAnEncryptedPdfReportsNothing() =
        runTest {
            val reporter = com.payslipmax.pdfparser.testing.FakeCrashReporter()
            val vm = PayslipViewModel(repository, crashReporter = reporter)
            fakeParser.result = Result.failure(Exception("UNRECOGNIZED_GRAMMAR in /data/user/0/x/Sunil-ABCDE1234F.pdf, net 98765.43"))

            vm.importPayslip(validPdfBytes, "pw", "Sunil-ABCDE1234F.pdf")

            val (event, keys) = reporter.exceptions.single()
            assertEquals("pdf_import_failed", event.message)
            assertEquals("UNRECOGNIZED_GRAMMAR", keys?.get("error_reason"))
            val everything = (listOf(event.toString()) + keys.orEmpty().keys + keys.orEmpty().values).joinToString("|")
            assertFalse("ABCDE1234F" in everything || "98765" in everything || "/data/user" in everything)

            fakeParser.result = Result.failure(Exception("PASSWORD_PROTECTED"))
            vm.importPayslip(validPdfBytes, "pw", "locked.pdf")
            assertEquals(1, reporter.exceptions.size, "an encrypted PDF is asked for its password, not reported")
        }
}
