package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.platform.PlatformSystemInfo
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelSupportExtensionsTest {
    private val environment =
        IssueReportEnvironment(
            appVersion = "1.2.2",
            installationId = "PMX-A7F39C",
            systemInfo =
                PlatformSystemInfo(
                    deviceModel = "Test Phone 9",
                    osVersion = "TestOS 42",
                    cpuArchitecture = "arm64-v8a",
                ),
        )

    private data class SentEmail(
        val to: String,
        val subject: String,
        val body: String,
    )

    private lateinit var viewModel: PayslipViewModel
    private val sent = mutableListOf<SentEmail>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val repository = PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined)
        viewModel =
            PayslipViewModel(
                repository,
                issueReportSender = { to, subject, body -> sent += SentEmail(to, subject, body) },
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportContainsDeviceVersionFieldsAndTheUsersDescription() {
        val report = viewModel.buildDiagnosticReport("App froze on import", environment)

        assertTrue(report.contains("App: 1.2.2"))
        assertTrue(report.contains("Installation ID: PMX-A7F39C"))
        assertTrue(report.contains("OS: TestOS 42"))
        assertTrue(report.contains("Device: Test Phone 9"))
        assertTrue(report.contains("Architecture: arm64-v8a"))
        assertTrue(report.contains("App froze on import"))
    }

    @Test
    fun reportRecordsWhereTheRequestWasRaisedInsteadOfNone() {
        val report = viewModel.buildDiagnosticReport("x", environment)

        assertTrue(report.contains("Last screen: ${AppStringsSupport.reportIssueScreenName}"))
        assertTrue(report.contains("Last operation: ${AppStringsSupport.reportIssueOperationName}"))
    }

    @Test
    fun panTypedIntoTheDescriptionNeverReachesTheReport() {
        // A support email leaves the device: a PAN typed by the user must be scrubbed first.
        val report = viewModel.buildDiagnosticReport("my PAN is ABCDE1234F, please check", environment)

        assertFalse(report.contains("ABCDE1234F"))
    }

    @Test
    fun accountNumberTypedIntoTheDescriptionNeverReachesTheReport() {
        // TelemetrySanitizer.sanitizeMessage() misses CDA account numbers; the report path must add it.
        val report = viewModel.buildDiagnosticReport("CDA account 123456789012 is wrong", environment)

        assertFalse(report.contains("123456789012"))
        assertTrue(report.contains("[REDACTED_ACCOUNT]"))
    }

    @Test
    fun submitBodyNeverCarriesAnAccountNumber() {
        viewModel.submitIssueReport("acct 5010012345678", environment)

        assertFalse(sent.single().body.contains("5010012345678"))
    }

    @Test
    fun blankDescriptionStillProducesADeliverableReport() {
        val report = viewModel.buildDiagnosticReport("   ", environment)

        assertTrue(report.contains("Installation ID: PMX-A7F39C"))
    }

    @Test
    fun submitSendsToTheSupportAddressWithVersionAndInstallationIdInTheSubject() {
        viewModel.submitIssueReport("Import fails", environment)

        val email = sent.single()
        assertEquals(AppStringsSupport.supportEmail, email.to)
        // Triagers filter their inbox by these two tokens.
        assertTrue(email.subject.contains("1.2.2"))
        assertTrue(email.subject.contains("PMX-A7F39C"))
        assertTrue(email.body.contains("Import fails"))
    }

    @Test
    fun submitBodyIsTheSanitizedReportNotTheRawDescription() {
        viewModel.submitIssueReport("PAN ABCDE1234F", environment)

        assertFalse(sent.single().body.contains("ABCDE1234F"))
    }
}
