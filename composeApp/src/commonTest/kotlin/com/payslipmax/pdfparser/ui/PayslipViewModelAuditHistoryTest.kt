package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.toCorrectionEntity
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The ledger is derived from the payslips the officer actually sees, so it must carry their corrections
 * (a new import of a corrected month is audited with them applied), and a ledger that is missing months
 * (a restore from before backups rebuilt it) is repaired once at startup, off the UI path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelAuditHistoryTest {
    private lateinit var dao: FakePayslipDao
    private lateinit var parser: FakePdfParser
    private lateinit var repository: PayslipRepository
    private lateinit var intelligence: CountingIntelligence

    private val pdf = "%PDF-1.4".encodeToByteArray()

    private class CountingIntelligence(
        dao: FakePayslipDao,
    ) : FinancialIntelligenceRepository(dao, Dispatchers.Unconfined) {
        var repairCalls = 0

        override suspend fun rebuildAuditHistory(payslips: List<ParsedPayslip>) {
            repairCalls++
            super.rebuildAuditHistory(payslips)
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = FakePayslipDao()
        parser = FakePdfParser()
        repository = PayslipRepository(dao, parser, Dispatchers.Unconfined)
        intelligence = CountingIntelligence(dao)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PayslipViewModel(repository, intelligence)

    private fun slip(dateStr: String) =
        dateStr.split("/").let { (m, y) ->
            ParsedPayslip(
                file = "p.pdf", year = y.toInt(), monthNum = m.toInt(), monthName = "", dateStr = dateStr,
                officer = Officer("N", "A", "P"),
                earnings = Earnings(basicPay = 100.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(100.0, 0.0, 100.0),
                taxAndSavings = null,
            )
        }

    @Test
    fun importingACorrectedMonthAuditsItWithTheCorrectionApplied() =
        runTest {
            dao.insertCorrection(mapOf("basicPay" to 77777.0).toCorrectionEntity("03/2017"))
            parser.result = Result.success(slip("03/2017"))

            viewModel().importPayslip(pdf, "", "p.pdf")

            assertEquals(77777.0, dao.getLedgerRecordByDate("03/2017")?.basicPay, "the ledger must hold the corrected value Pay Audit shows, not the raw parse")
        }

    @Test
    fun theFilePickerImportPathAuditsWithCorrectionsToo() =
        runTest {
            dao.insertCorrection(mapOf("basicPay" to 66666.0).toCorrectionEntity("04/2017"))
            parser.result = Result.success(slip("04/2017"))

            viewModel().onFilePicked(pdf, "p.pdf")

            assertEquals(66666.0, dao.getLedgerRecordByDate("04/2017")?.basicPay)
        }

    @Test
    fun startupRepairsALedgerThatIsMissingMonthsExactlyOnce() =
        runTest {
            listOf("01/2018", "02/2018", "03/2018").forEach { dao.insertPayslip(slip(it).toEncryptedEntity()) }
            intelligence.processPayslipAndRunAnalysis(slip("01/2018"))
            intelligence.repairCalls = 0

            val vm = viewModel()

            assertEquals(listOf("01/2018", "02/2018", "03/2018"), dao.getAllLedgerRecords().first().map { it.dateStr })
            assertEquals(1, intelligence.repairCalls)
            dao.insertPayslip(slip("04/2018").toEncryptedEntity())
            assertEquals(1, intelligence.repairCalls, "a later payslip emission must not re-run the repair (${vm.uiState.value.payslips.size} payslips)")
        }

    @Test
    fun startupLeavesACompleteLedgerAlone() =
        runTest {
            listOf("01/2018", "02/2018").forEach {
                dao.insertPayslip(slip(it).toEncryptedEntity())
                intelligence.processPayslipAndRunAnalysis(slip(it))
            }
            intelligence.repairCalls = 0

            viewModel()

            assertEquals(0, intelligence.repairCalls)
        }
}
