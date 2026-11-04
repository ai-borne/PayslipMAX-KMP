package com.payslipmax.pdfparser.ui.backup

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DsopFund
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.part2.data.InMemoryLastBackupStorage
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.space.FreeSpaceGate
import com.payslipmax.pdfparser.repository.BackupTelemetry
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakeBackupFiles
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeFreeSpaceReader
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * A [BackupRestoreViewModel] built the way the app graph builds it (sharedModule + appModule): the service has the Orders Bank backup, the
 * last-backup storage, the free-space gate, the ledger rebuild and the telemetry, each over one [FakePayslipDao]. Only the leaves are fakes.
 * The view model sits in a [ViewModelStore], so [clear] is the framework's own `onCleared` path.
 */
internal class BackupViewModelHarness(
    val dao: FakePayslipDao = FakePayslipDao(),
    // The service's own worker dispatcher; production runs it off the main thread (Dispatchers.Default).
    serviceDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) {
    val files = FakeBackupFiles()
    val reporter = FakeCrashReporter()
    val lastBackup = InMemoryLastBackupStorage()
    val space = FakeFreeSpaceReader()
    var clock = 1_788_000_000_000L
    private val parser = FakePdfParser()
    val repository = PayslipRepository(dao, parser, Dispatchers.Unconfined)
    val service =
        PayslipBackupService(
            dao,
            serviceDispatcher,
            payslipRepository = repository,
            intelligence = FinancialIntelligenceRepository(dao, Dispatchers.Unconfined),
            part2Backup = Part2Backup(dao.part2),
            lastBackup = lastBackup,
            now = { clock },
            space = FreeSpaceGate(space),
        )
    private val store = ViewModelStore()

    /** Built on first use, so a test can set the leaves (a last backup time, say) first. */
    val viewModel: BackupRestoreViewModel by lazy {
        val factory = viewModelFactory { initializer { BackupRestoreViewModel(service, files, BackupTelemetry(reporter, dao), lastBackup) } }
        ViewModelProvider.create(store, factory)[BackupRestoreViewModel::class]
    }

    /** Every state the view model showed, from the moment [record] is called. */
    val states = mutableListOf<BackupUiState>()

    /** Collects on an unconfined dispatcher, so every state is seen as it is set, not only the last one before the test yields. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun record(test: TestScope) {
        test.backgroundScope.launch(UnconfinedTestDispatcher(test.testScheduler)) { viewModel.state.collect { states += it } }
    }

    /** The framework clearing the view model (its owner is gone): `onCleared`. */
    fun clear() = store.clear()

    suspend fun seedPayslip(dateStr: String) {
        parser.result = Result.success(payslip(dateStr))
        repository.importPayslip(byteArrayOf(1, 2, 3), "pdf-pw", "upload.pdf")
    }

    suspend fun payslipDates(): List<String> = repository.getAllPayslips().first().map { it.dateStr }

    /** A whole backup of what the device holds now, staged as the picker would hand it over. */
    suspend fun pickedBackupOfTheDevice(password: String = PASSWORD): String = files.stage(service.export(Secret.of(password)).getOrThrow())

    companion object {
        const val PASSWORD = "Pass#Secret-4321"

        fun payslip(dateStr: String): ParsedPayslip {
            val (month, year) = dateStr.split("/").map { it.toInt() }
            return ParsedPayslip(
                file = "payslip.pdf", year = year, monthNum = month, monthName = "Month_$month", dateStr = dateStr,
                officer = Officer("Name", "Acc", "PAN"),
                earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
                summary = PayslipSummary(100.0, 80.0, 20.0),
                taxAndSavings =
                    TaxAndSavings(
                        grossSalaryYtd = 1000.0,
                        totalTaxableIncome = 900.0,
                        standardDeduction = 50.0,
                        netTaxableIncome = 850.0,
                        totalTaxPayable = 100.0,
                        taxDeductedYtd = 80.0,
                        cessDeductedYtd = 20.0,
                        dsopFund = DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0),
                    ),
            )
        }
    }
}
