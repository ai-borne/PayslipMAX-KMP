package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.repository.BackupTelemetry
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakeBackupFiles
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P6, F5): the view model is where a backup or restore ends, share sheet and picked file included, so it is where the
 * one non-fatal is sent: code-only keys for a failure, nothing for a success, and nothing when the officer turned telemetry off.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelBackupTelemetryTest {
    private val files = FakeBackupFiles()
    private val reporter = FakeCrashReporter()
    private val dao = FakePayslipDao()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel =
            PayslipViewModel(
                PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined),
                backupService = PayslipBackupService(dao, Dispatchers.Unconfined),
                backupFiles = files,
                backupTelemetry = BackupTelemetry(reporter, dao),
            )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun sentCodes() = reporter.exceptions.map { (_, keys) -> keys!!.getValue("backup_op") to keys.getValue("backup_failure_code") }

    @Test
    fun aBackupThatCannotBeSharedSendsItsCodeAndAWorkingOneSendsNothing() =
        runTest {
            viewModel.exportBackup("Pass#12345") {}
            runCurrent()
            assertTrue(reporter.exceptions.isEmpty(), "a backup that worked sends nothing")

            files.shareFails = true
            viewModel.exportBackup("Pass#12345") {}
            runCurrent()

            assertEquals(listOf("backup" to "write_failed"), sentCodes())
            assertEquals("6", reporter.exceptions.single().second!!.getValue("backup_format_version"))
        }

    @Test
    fun aRestoreWithTheWrongFileSendsItsCode() =
        runTest {
            val picked = files.stage(ByteArray(300) { 7 })
            viewModel.importBackup(picked, "Pass#12345", RestoreMode.REPLACE) {}
            runCurrent()

            assertEquals(listOf("restore" to "wrong_password"), sentCodes())
        }

    @Test
    fun nothingIsSentWhenTheOfficerTurnedTelemetryOff() =
        runTest {
            dao.insertSettings(AppSettingsEntity(isTelemetryEnabled = false))
            files.shareFails = true
            viewModel.exportBackup("Pass#12345") {}
            viewModel.importBackup(files.stage(ByteArray(300) { 7 }), "Pass#12345", RestoreMode.MERGE) {}
            runCurrent()

            assertTrue(reporter.exceptions.isEmpty() && reporter.logs.isEmpty() && reporter.keys.isEmpty())
        }
}
