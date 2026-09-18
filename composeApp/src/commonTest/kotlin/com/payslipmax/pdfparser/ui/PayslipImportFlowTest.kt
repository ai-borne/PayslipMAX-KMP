package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DsopFund
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.rating.RatingPromptManager
import com.payslipmax.pdfparser.rating.RatingPromptStorage
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.theme.ImportStrings
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipImportFlowTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    private val validPdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x35) // "%PDF-1.5"
    private val invalidPdfBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("500 B", formatFileSize(500))
        assertEquals("142 KB", formatFileSize(142 * 1024))
        assertEquals("2.0 MB", formatFileSize(2 * 1024 * 1024))
    }

    @Test
    fun testIsValidPdfBytes() {
        assertTrue(isValidPdfBytes(validPdfBytes))
        assertFalse(isValidPdfBytes(invalidPdfBytes))
        assertFalse(isValidPdfBytes(byteArrayOf(0x25, 0x50))) // Too short
    }

    @Test
    fun testFilePicked_invalidPdfHeader_transitionsToFailure() {
        viewModel.onFilePicked(invalidPdfBytes, "bad_file.pdf")

        val state = viewModel.uiState.value.importUiState
        assertTrue(state is ImportUiState.Failure)
        assertEquals(ImportStrings.errInvalidPdf, state.message)
        assertNull(viewModel.pendingImportPdfBytes)
    }

    @Test
    fun testFilePicked_unencryptedPdf_importsDirectlyWithoutPasswordPrompt() =
        runTest {
            val mock = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mock)

            viewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")

            val state = viewModel.uiState.value.importUiState
            assertTrue(state is ImportUiState.Success)
            assertEquals(mock, state.payslip)
            assertTrue(viewModel.uiState.value.importSuccess)
            assertNull(viewModel.pendingImportPdfBytes)
        }

    @Test
    fun testFilePicked_encryptedPdf_transitionsToPasswordRequired() =
        runTest {
            fakeParser.result = Result.failure(Exception("PASSWORD_PROTECTED: Document is encrypted"))

            viewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")

            val state = viewModel.uiState.value.importUiState
            assertTrue(state is ImportUiState.PasswordRequired)
            assertEquals("payslip_aug_2024.pdf", state.fileName)
            assertNull(state.errorMessage)
            assertEquals("", state.passwordInput)
            assertFalse(state.isDecrypting)
            assertNotNull(viewModel.pendingImportPdfBytes)
        }

    @Test
    fun testPasswordChangeAndVisibilityToggle() =
        runTest {
            fakeParser.result = Result.failure(Exception("PASSWORD_PROTECTED"))
            viewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")

            viewModel.onImportPasswordChanged("myPassword123")
            var state = viewModel.uiState.value.importUiState as ImportUiState.PasswordRequired
            assertEquals("myPassword123", state.passwordInput)
            assertFalse(state.isPasswordVisible)

            viewModel.onToggleImportPasswordVisibility()
            state = viewModel.uiState.value.importUiState as ImportUiState.PasswordRequired
            assertTrue(state.isPasswordVisible)
        }

    @Test
    fun testSubmitPassword_incorrectPassword_setsInlineError() =
        runTest {
            fakeParser.result = Result.failure(Exception("PASSWORD_PROTECTED"))
            viewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")

            viewModel.onImportPasswordChanged("wrongPass")
            fakeParser.result = Result.failure(Exception("InvalidPasswordException: Password is wrong"))

            viewModel.onSubmitImportPassword()

            val state = viewModel.uiState.value.importUiState
            assertTrue(state is ImportUiState.PasswordRequired)
            assertEquals(ImportStrings.errIncorrectPassword, state.errorMessage)
            assertFalse(state.isDecrypting)
            // Memory guardrail: Cached bytes remain preserved for retry
            assertNotNull(viewModel.pendingImportPdfBytes)
        }

    @Test
    fun testSubmitPassword_correctPassword_succeedsAndClearsMemory() =
        runTest {
            fakeParser.result = Result.failure(Exception("PASSWORD_PROTECTED"))
            viewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")

            viewModel.onImportPasswordChanged("correctPass")
            val mock = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mock)

            viewModel.onSubmitImportPassword()

            val state = viewModel.uiState.value.importUiState
            assertTrue(state is ImportUiState.Success)
            assertEquals(mock, state.payslip)
            assertTrue(viewModel.uiState.value.importSuccess)
            // Cybersecurity: Cached PDF bytes wiped from memory
            assertNull(viewModel.pendingImportPdfBytes)
        }

    @Test
    fun testDismissImport_clearsMemoryState() =
        runTest {
            fakeParser.result = Result.failure(Exception("PASSWORD_PROTECTED"))
            viewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")
            assertNotNull(viewModel.pendingImportPdfBytes)

            viewModel.onDismissImport()

            val state = viewModel.uiState.value.importUiState
            assertTrue(state is ImportUiState.Idle)
            assertNull(viewModel.pendingImportPdfBytes)
            assertNull(viewModel.pendingImportFilename)
        }

    private class FakeRatingPromptStorage(
        private var cleanSuccessCount: Int = 0,
        private var lastPromptTimestampMs: Long? = null,
        private var hasEverPrompted: Boolean = false,
    ) : RatingPromptStorage {
        var saveCleanSuccessCountCalls = 0
            private set

        override fun getCleanSuccessCount(): Int = cleanSuccessCount

        override fun saveCleanSuccessCount(count: Int) {
            cleanSuccessCount = count
            saveCleanSuccessCountCalls++
        }

        override fun getLastPromptTimestampMs(): Long? = lastPromptTimestampMs

        override fun saveLastPromptTimestampMs(timestampMs: Long) {
            lastPromptTimestampMs = timestampMs
        }

        override fun getHasEverPrompted(): Boolean = hasEverPrompted

        override fun saveHasEverPrompted(hasPrompted: Boolean) {
            hasEverPrompted = hasPrompted
        }
    }

    @Test
    fun maybePromptForRating_eligibleCleanNewImport_invokesReviewRequester() =
        runTest {
            val storage = FakeRatingPromptStorage(cleanSuccessCount = RatingPromptManager.FIRST_PROMPT_THRESHOLD - 1)
            val testViewModel = PayslipViewModel(repository, ratingPromptManager = RatingPromptManager(storage))
            var requestCount = 0
            testViewModel.reviewRequester = { requestCount++ }
            val mock = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mock)

            testViewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")
            testViewModel.maybePromptForRating(mock)

            assertEquals(1, requestCount)
        }

    @Test
    fun maybePromptForRating_duplicateReimport_doesNotCallOnCleanParseSuccess() =
        runTest {
            val storage = FakeRatingPromptStorage()
            val testViewModel = PayslipViewModel(repository, ratingPromptManager = RatingPromptManager(storage))
            var requestCount = 0
            testViewModel.reviewRequester = { requestCount++ }
            val mock = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mock)

            testViewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf") // new payslip
            testViewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf") // duplicate re-import
            testViewModel.maybePromptForRating(mock)

            assertEquals(0, storage.saveCleanSuccessCountCalls)
            assertEquals(0, requestCount)
        }

    @Test
    fun maybePromptForRating_dirtyParse_doesNotCallOnCleanParseSuccess() =
        runTest {
            val storage = FakeRatingPromptStorage()
            val testViewModel = PayslipViewModel(repository, ratingPromptManager = RatingPromptManager(storage))
            var requestCount = 0
            testViewModel.reviewRequester = { requestCount++ }
            val dirty = createMockPayslip("08/2024").copy(needsReview = true)
            fakeParser.result = Result.success(dirty)

            testViewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")
            testViewModel.maybePromptForRating(dirty)

            assertEquals(0, storage.saveCleanSuccessCountCalls)
            assertEquals(0, requestCount)
        }

    @Test
    fun maybePromptForRating_belowThreshold_doesNotInvokeReviewRequester() =
        runTest {
            val storage = FakeRatingPromptStorage(cleanSuccessCount = 0)
            val testViewModel = PayslipViewModel(repository, ratingPromptManager = RatingPromptManager(storage))
            var requestCount = 0
            testViewModel.reviewRequester = { requestCount++ }
            val mock = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mock)

            testViewModel.onFilePicked(validPdfBytes, "payslip_aug_2024.pdf")
            testViewModel.maybePromptForRating(mock)

            assertEquals(0, requestCount)
            assertEquals(1, storage.getCleanSuccessCount())
        }

    private fun createMockPayslip(dateStr: String) =
        dateStr.split("/").let { split ->
            val month = split[0].toInt()
            val year = split[1].toInt()
            ParsedPayslip(
                file = "payslip_$dateStr.pdf", year = year, monthNum = month, monthName = "Month_$month", dateStr = dateStr,
                officer = Officer("Name", "Acc", "PAN"),
                earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
                summary = PayslipSummary(100.0, 80.0, 20.0),
                taxAndSavings = TaxAndSavings(1000.0, 900.0, 50.0, 850.0, 100.0, 80.0, 20.0, DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0)),
            )
        }
}
