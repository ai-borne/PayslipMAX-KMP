package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.repository.BackupFailureEvent
import com.payslipmax.pdfparser.repository.BackupTelemetry
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.telemetry.TelemetrySanitizer
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import com.payslipmax.pdfparser.ui.screens.BackupContents
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P6 and P7, F5): the view model is where a backup or restore ends, share sheet and picked file included, so it is where
 * the one non-fatal is sent: code-only keys for a failure, nothing for a success or a cancelled preview, and nothing at all when the officer
 * turned telemetry off. A commit that fails knows the file's format from its preview (P10 "From P6" item 4), so it says which format failed.
 * Moved from PayslipViewModelBackupTelemetryTest, which drove the PayslipViewModel extensions P7 deleted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTelemetryTest {
    private val harness = BackupViewModelHarness()
    private val contents = BackupContents(payslips = 1, part2Items = 0)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    private fun sent() = harness.reporter.exceptions.map { (event, keys) -> assertIs<BackupFailureEvent>(event).let { keys!! } }

    private fun sentCodes() = sent().map { it.getValue(BackupTelemetry.KEY_OP) to it.getValue(BackupTelemetry.KEY_FAILURE) }

    @Test
    fun aBackupThatCannotBeSharedSendsItsCodeAndTheCurrentFormatAndAWorkingOneSendsNothing() =
        runTest {
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)
            assertTrue(harness.reporter.exceptions.isEmpty(), "a backup that worked sends nothing")

            harness.files.shareFails = true
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)

            assertEquals(listOf("backup" to "write_failed"), sentCodes())
            assertEquals(PortableBackup.CURRENT_VERSION.toString(), sent().single().getValue(BackupTelemetry.KEY_FORMAT))
        }

    @Test
    fun aRestoreWithTheWrongFileSendsItsCodeWithAnUnknownFormat() =
        runTest {
            harness.viewModel.restore(harness.files.stage(ByteArray(300) { 7 }), Secret.of(PASSWORD))

            assertEquals(listOf("restore" to "wrong_password"), sentCodes())
            assertEquals("unknown", sent().single().getValue(BackupTelemetry.KEY_FORMAT))
        }

    @Test
    fun aCommitThatFailsSendsThePreviewsFormatAndACancelledPreviewSendsNothing() =
        runTest {
            harness.dao.insertSettings(AppSettingsEntity())
            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), Secret.of(PASSWORD))
            harness.viewModel.cancel()
            assertTrue(harness.reporter.exceptions.isEmpty(), "a cancel is not a failure")

            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), Secret.of(PASSWORD))
            val format = assertIs<BackupUiState.Preview>(harness.viewModel.state.value).preview.formatVersion
            harness.dao.failInsertSettingsWhen = { true }
            harness.viewModel.commit(RestoreMode.REPLACE)

            assertEquals(listOf("restore" to "unknown"), sentCodes())
            assertEquals(format.toString(), sent().single().getValue(BackupTelemetry.KEY_FORMAT))
        }

    @Test
    fun everyKeySentIsAllowedAndEveryValueIsAFixedCode() =
        runTest {
            harness.files.shareFails = true
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)
            harness.viewModel.restore(harness.files.stage(ByteArray(300) { 7 }), Secret.of(PASSWORD))

            val codes = setOf("backup", "restore", "write_failed", "wrong_password", "unknown", PortableBackup.CURRENT_VERSION.toString())
            for ((key, value) in sent().flatMap { it.entries }.map { it.toPair() }) {
                assertTrue(TelemetrySanitizer.isKeyAllowed(key), key)
                assertTrue(value in codes, "$key carried a value that is not a fixed code")
            }
            assertEquals(2, sent().size, "one report per failure")
        }

    @Test
    fun nothingIsSentWhenTheOfficerTurnedTelemetryOff() =
        runTest {
            harness.dao.insertSettings(AppSettingsEntity(isTelemetryEnabled = false))
            harness.files.shareFails = true
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)
            harness.viewModel.restore(harness.files.stage(ByteArray(300) { 7 }), Secret.of(PASSWORD))

            // A failed commit is not driven here: the fake DAO is not transactional, so a REPLACE that fails has already cleared the
            // settings row holding the choice (Room rolls it back). Every path reports through the same BackupTelemetry, which reads it.
            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertTrue(harness.reporter.exceptions.isEmpty() && harness.reporter.logs.isEmpty() && harness.reporter.keys.isEmpty())
        }
}
