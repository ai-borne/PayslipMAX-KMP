package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.SyntheticPart2Backups
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import com.payslipmax.pdfparser.ui.screens.BackupContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, section 4): the view model is handed the officer's password as a [Secret] and owns it from then on, so it must
 * be wiped however the call ends: when the work finishes, when a second tap is ignored, when the work fails, when a preview is left and
 * when the view model's scope is cancelled before the work ever started. Each test keeps the `CharArray` the secret was made from, so
 * "wiped" means those bytes are zero. The main dispatcher runs only when told to, so a wipe left to a later dispatch is caught.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelSecretTest {
    private val harness = BackupViewModelHarness()
    private val contents = BackupContents(payslips = 1, part2Items = 0)

    private class Typed(
        text: String,
    ) {
        val chars = text.toCharArray()
        val secret get() = Secret.adopt(chars)
        val isWiped get() = chars.all { it == '\u0000' }
    }

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    private fun TestScope.queued() = Dispatchers.setMain(StandardTestDispatcher(testScheduler))

    @Test
    fun aBackupWipesThePasswordWhenItFinishesAndAFailedOneDoesToo() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            val good = Typed(PASSWORD)
            val failing = Typed(PASSWORD)

            harness.viewModel.backUp(good.secret, contents)
            advanceUntilIdle()
            harness.files.shareFails = true
            harness.viewModel.dismiss()
            harness.viewModel.backUp(failing.secret, contents)
            advanceUntilIdle()

            assertTrue(good.isWiped, "a backup that worked")
            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertTrue(failing.isWiped, "a backup that failed")
        }

    @Test
    fun aSecondBackUpWhileOneIsWorkingIsIgnoredButItsPasswordIsStillWiped() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            val first = Typed(PASSWORD)
            val ignored = Typed(PASSWORD)

            harness.viewModel.backUp(first.secret, contents)
            harness.viewModel.backUp(ignored.secret, contents)

            assertIs<BackupUiState.Working>(harness.viewModel.state.value)
            assertTrue(ignored.isWiped, "an ignored tap must not leave its password behind")
            assertFalse(first.isWiped, "the running backup still needs its own")
            advanceUntilIdle()
            assertTrue(first.isWiped)
        }

    @Test
    fun aRestorePickedWhileBusyIsIgnoredItsFileDeletedAndItsPasswordWiped() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            harness.viewModel.backUp(Typed(PASSWORD).secret, contents)
            val ignored = Typed(PASSWORD)
            val picked = harness.pickedBackupOfTheDevice()

            harness.viewModel.restore(picked, ignored.secret)

            assertTrue(ignored.isWiped)
            assertTrue(picked !in harness.files.pickedKeys, "the file of an ignored pick is deleted")
        }

    @Test
    fun aViewModelClearedBeforeItsBackupStartedStillWipesThePassword() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            val typed = Typed(PASSWORD)

            harness.viewModel.backUp(typed.secret, contents)
            harness.clear()
            advanceUntilIdle()

            assertTrue(typed.isWiped, "the scope was cancelled before the backup's first line ran")
        }

    @Test
    fun aViewModelClearedBeforeItsRestoreStartedStillWipesThePassword() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            val typed = Typed(PASSWORD)
            val picked = harness.pickedBackupOfTheDevice()
            harness.viewModel // built before it is cleared
            harness.clear()

            harness.viewModel.restore(picked, typed.secret)
            advanceUntilIdle()

            assertTrue(typed.isWiped, "a restore started on a view model whose scope is already cancelled never runs")
        }

    @Test
    fun aFormat6RestoreWipesThePasswordWhileThePreviewIsStillOnScreen() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            val typed = Typed(PASSWORD)

            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), typed.secret)
            advanceUntilIdle()

            assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
            assertTrue(typed.isWiped, "the archive key replaces the password, so the preview needs none")
            harness.viewModel.cancel()
        }

    @Test
    fun anOlderFilesPasswordLivesOnlyUntilThePreviewIsLeftByAnyExit() =
        runTest {
            queued()
            val older = SyntheticPart2Backups.withUnreadableRows(PASSWORD, unreadableEntries = 0, unreadableOrders = 0)
            val cancelled = Typed(PASSWORD)
            val committed = Typed(PASSWORD)

            harness.viewModel.restore(harness.files.stage(older), cancelled.secret)
            advanceUntilIdle()
            assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
            assertFalse(cancelled.isWiped, "formats 1 to 5 seal each row with the password, so a MERGE after the preview needs it")
            harness.viewModel.cancel()
            advanceUntilIdle()
            assertTrue(cancelled.isWiped, "Cancel")

            harness.viewModel.restore(harness.files.stage(older), committed.secret)
            advanceUntilIdle()
            harness.viewModel.commit(RestoreMode.MERGE)
            advanceUntilIdle()
            assertTrue(committed.isWiped, "a commit")
        }

    @Test
    fun aRestoreThatFailsWipesThePasswordWhateverWentWrong() =
        runTest {
            queued()
            harness.seedPayslip("08/2024")
            val wrong = Typed("not-the-password")

            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), wrong.secret)
            advanceUntilIdle()

            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertTrue(wrong.isWiped)
        }
}
