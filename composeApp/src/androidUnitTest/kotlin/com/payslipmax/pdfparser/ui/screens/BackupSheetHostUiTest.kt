package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * WHY (docs/Plan/14a P8, D10): the sheet is wired to the real view model here, as the app wires it. A restore onto an empty device used to
 * commit Replace by itself the moment its preview opened, so the officer never saw what the file held. The preview must wait for the
 * officer's answer on every device, and only that answer writes.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSheetHostUiTest {
    private companion object {
        const val SHEET_SETTLE_MS = 2_000L
    }

    private val harness = BackupViewModelHarness()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun aRestoreOnAnEmptyDeviceWaitsAtThePreviewForTheOfficersAnswer() =
        runComposeUiTest {
            runBlocking {
                harness.seedPayslip("08/2024")
                val picked = harness.pickedBackupOfTheDevice()
                harness.repository.clearAll()
                harness.viewModel.restore(picked, Secret.of(PASSWORD))
            }
            setContent {
                BackupRestoreSettingsCard(
                    backup = harness.viewModel,
                    contents = { BackupContents(payslips = 0, part2Items = 0) },
                    onPickBackup = {},
                    canBackup = true,
                    onUpgradePrompt = {},
                )
            }
            waitForIdle()
            onNodeWithTag(RestorePreviewCardTag).assertIsDisplayed()
            assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
            assertEquals(emptyList(), runBlocking { harness.payslipDates() }, "nothing is written before the answer")

            mainClock.advanceTimeBy(SHEET_SETTLE_MS) // the sheet slides in; a tap before it settles lands off the button
            onNodeWithTag(RestoreConfirmButtonTag).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            assertIs<BackupUiState.Done>(harness.viewModel.state.value)
            assertEquals(listOf("08/2024"), runBlocking { harness.payslipDates() })
        }

    /**
     * WHY (docs/Plan/14a P10): the system file picker can stay open for a long time, or never answer. A copy of the password made when
     * Restore is pressed would wait on it (and leak if the officer cancels). The password is copied out of the field when the file
     * arrives, so what the officer has typed by then is what is used.
     */
    @Test
    fun aRestoreReadsThePasswordWhenTheFileArrivesNotWhenTheButtonIsPressed() =
        runComposeUiTest {
            var fileArrives: ((String) -> Unit)? = null
            val picked =
                runBlocking {
                    harness.seedPayslip("08/2024")
                    harness.pickedBackupOfTheDevice()
                }
            setContent {
                BackupRestoreSettingsCard(
                    backup = harness.viewModel,
                    contents = { BackupContents(payslips = 1, part2Items = 0) },
                    onPickBackup = { fileArrives = it },
                    canBackup = true,
                    onUpgradePrompt = {},
                )
            }
            onNodeWithText(AppStrings.settingsRowBackupLabel).performClick()
            mainClock.advanceTimeBy(SHEET_SETTLE_MS)
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput(PASSWORD)

            onNodeWithTag(RestoreButtonTag).performSemanticsAction(SemanticsActions.OnClick)
            val deliver = assertNotNull(fileArrives, "pressing Restore opens the picker")
            assertIs<BackupUiState.Idle>(harness.viewModel.state.value, "nothing runs until a file is picked")
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("X")
            deliver(picked)
            waitForIdle()

            assertEquals(BackupUiState.Failed(BackupFailure.WRONG_PASSWORD), harness.viewModel.state.value, "the password typed after the press is the one used")
        }

    @Test
    fun restoreWithNoPasswordOpensNoPickerAndSaysAPasswordIsNeeded() =
        runComposeUiTest {
            var pickerOpened = false
            setContent {
                BackupRestoreSettingsCard(
                    backup = harness.viewModel,
                    contents = { BackupContents(payslips = 1, part2Items = 0) },
                    onPickBackup = { pickerOpened = true },
                    canBackup = true,
                    onUpgradePrompt = {},
                )
            }
            onNodeWithText(AppStrings.settingsRowBackupLabel).performClick()
            mainClock.advanceTimeBy(SHEET_SETTLE_MS)
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("   ")

            onNodeWithTag(RestoreButtonTag).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            assertEquals(false, pickerOpened, "a password of spaces is no password")
            onNodeWithText(AppStrings.settingsBackupPasswordRequired).assertExists()
        }
}
