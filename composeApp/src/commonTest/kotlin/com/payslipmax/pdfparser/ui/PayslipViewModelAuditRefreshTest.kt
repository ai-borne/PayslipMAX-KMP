package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipRepository
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
import kotlin.test.assertTrue

/**
 * The ledger feeds Pay Audit, so it must follow the values the officer sees. A saved correction and a
 * parser-fixing re-parse both change what a month's payslip holds, so both must refresh that month's ledger
 * row; otherwise Pay Audit judges stale figures until the next restore or import.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelAuditRefreshTest {
    private lateinit var dao: FakePayslipDao
    private lateinit var parser: FakePdfParser
    private lateinit var repository: PayslipRepository
    private lateinit var intelligence: FinancialIntelligenceRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = FakePayslipDao()
        parser = FakePdfParser()
        repository = PayslipRepository(dao, parser, Dispatchers.Unconfined)
        intelligence = FinancialIntelligenceRepository(dao, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun slip(
        dateStr: String,
        basicPay: Double = 100.0,
    ) = dateStr.split("/").let { (m, y) ->
        ParsedPayslip(
            file = "p.pdf", year = y.toInt(), monthNum = m.toInt(), monthName = "", dateStr = dateStr,
            officer = Officer("N", "A", "P"),
            earnings = Earnings(basicPay = basicPay),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(basicPay, 0.0, basicPay),
            taxAndSavings = null,
        )
    }

    private suspend fun storeAndAudit(parsed: ParsedPayslip) {
        dao.insertPayslip(parsed.toEncryptedEntity())
        intelligence.processPayslipAndRunAnalysis(parsed)
    }

    @Test
    fun savingACorrectionRefreshesThatMonthsLedgerRow() =
        runTest {
            storeAndAudit(slip("03/2017"))
            val vm = PayslipViewModel(repository, intelligence)
            assertEquals(100.0, dao.getLedgerRecordByDate("03/2017")?.basicPay)

            vm.updateDraftCorrection(SingleCorrection("basicPay", "BPAY", 55555.0, EntryCategory.EARNING, CorrectionType.EDITED, 100.0, "BPAY", 0L))
            vm.saveEditingSession("03/2017")

            assertEquals(55555.0, dao.getLedgerRecordByDate("03/2017")?.basicPay, "Pay Audit must judge the corrected value, not the old parse")
        }

    @Test
    fun reparsingRefreshesTheLedgerWithTheFixedParse() =
        runTest {
            storeAndAudit(slip("03/2017", basicPay = 1.0))
            dao.insertPayslipPdf(com.payslipmax.pdfparser.database.PayslipPdfEntity("03/2017", byteArrayOf(1)))
            parser.resultsByFilename = mapOf("03/2017.pdf" to Result.success(slip("03/2017", basicPay = 9999.0)))
            val vm = PayslipViewModel(repository, intelligence)
            var summary: Result<*>? = null

            vm.reparseAllPayslips("pw") { summary = it }

            assertTrue(summary?.isSuccess == true)
            assertEquals(9999.0, dao.getLedgerRecordByDate("03/2017")?.basicPay, "a parser fix must reach Pay Audit, not only the payslip list")
        }

    @Test
    fun aFailingRefreshNeverFailsTheCorrectionSave() =
        runTest {
            storeAndAudit(slip("03/2017"))
            val broken =
                object : FinancialIntelligenceRepository(dao, Dispatchers.Unconfined) {
                    override suspend fun rebuildAuditHistory(payslips: List<ParsedPayslip>): Unit = error("boom")
                }
            val vm = PayslipViewModel(repository, broken)

            vm.updateDraftCorrection(SingleCorrection("basicPay", "BPAY", 4444.0, EntryCategory.EARNING, CorrectionType.EDITED, 100.0, "BPAY", 0L))
            vm.saveEditingSession("03/2017")

            assertEquals(4444.0, repository.getPayslipByDate("03/2017")?.earnings?.basicPay, "the correction is saved even if the audit refresh fails")
            assertTrue(!vm.uiState.value.isLoading && !vm.uiState.value.isEditModeActive)
        }
}
