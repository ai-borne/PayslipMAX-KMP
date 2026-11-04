package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.RestoreMode
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P7, section 4): a state is logged, shown and kept across the sheet closing, so it must hold nothing an attacker or a
 * crash log could use: never the password (it is passed per call and kept nowhere), never the picked file's name or path, never an
 * exception or its text. A failure is its code and nothing more, so two failures of one kind are the same state whatever they said.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupUiStateLeakTest {
    private val harness = BackupViewModelHarness()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun noStateNorTheViewModelEverHoldsThePasswordAPathOrExceptionText() =
        runTest {
            harness.dao.insertSettings(AppSettingsEntity())
            harness.seedPayslip("08/2024")
            harness.record(this)
            val viewModel = harness.viewModel
            val contents = BackupContents(payslips = 1, part2Items = 0)

            viewModel.backUp(PASSWORD, contents)
            viewModel.dismiss()
            harness.files.shareFails = true
            viewModel.backUp(PASSWORD, contents)
            viewModel.restore(harness.files.stage(ByteArray(300) { 7 }), PASSWORD)
            viewModel.restore(harness.pickedBackupOfTheDevice(), PASSWORD)
            viewModel.cancel()
            viewModel.restore(harness.pickedBackupOfTheDevice(), PASSWORD)
            harness.dao.failInsertSettingsWhen = { true }
            viewModel.commit(RestoreMode.REPLACE)

            assertEquals(BackupUiState.Failed(BackupFailure.UNKNOWN), viewModel.state.value, "the failed commit is its code only")
            val kinds = harness.states.map { it::class }.toSet()
            assertTrue(kinds.size == 5, "every kind of state was reached: $kinds")
            for (text in harness.states.map { it.toString() } + viewModel.toString()) {
                for (secret in listOf(PASSWORD, "Secret", "picked", "Simulated", "Exception", "/", "08/2024")) {
                    assertFalse(secret in text, "'$secret' reached a state")
                }
            }
        }
}
