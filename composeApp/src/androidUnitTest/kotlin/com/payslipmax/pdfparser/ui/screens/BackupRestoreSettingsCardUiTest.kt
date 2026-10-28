package com.payslipmax.pdfparser.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 56): found on the Pixel 9 release, an officer who had only used Part II (drafts and scanned orders, no payslip) was
 * told "No payslips to back up yet" and, on restore, had the vault replaced without being asked. The screen must read what is on the
 * device when a button is pressed and follow [BackupContents]: Back Up proceeds on Part II data alone, still refuses an empty device,
 * and Restore asks Replace or Merge whenever anything is there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class BackupRestoreSettingsCardUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var exports = 0
    private val restores = mutableListOf<RestoreMode>()

    private fun open(
        contents: BackupContents,
        password: String = "a made-up password",
    ) {
        composeRule.setContent {
            BackupRestoreSettingsCard(
                password = password,
                onPasswordChange = {},
                contents = { contents },
                // never completes: the share sheet is not under test
                onExportBackup = { _, _ -> exports++ },
                onRestore = { _, _, mode, _ -> restores += mode },
                onPickBackup = { onResult -> onResult(byteArrayOf(1)) },
                canBackup = true,
                onUpgradePrompt = {},
            )
        }
        composeRule.onNodeWithText(AppStrings.settingsRowBackupLabel).performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun backUpGoesAheadWithPartTwoItemsAndNoPayslips() {
        open(BackupContents(payslips = 0, part2Items = 2))

        composeRule.onNodeWithText(AppStrings.settingsBackupSaveBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(1, exports)
    }

    @Test
    fun backUpOnAnEmptyDeviceIsRefusedWithTheShippedLineAndExportsNothing() {
        open(BackupContents(payslips = 0, part2Items = 0))

        composeRule.onNodeWithText(AppStrings.settingsBackupSaveBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(AppStrings.statusNoPayslipsToBackup).assertExists()
        assertEquals(0, exports)
    }

    @Test
    fun restoreOnADeviceWithOnlyPartTwoItemsAsksFirstAndNamesThem() {
        open(BackupContents(payslips = 0, part2Items = 2))

        composeRule.onNodeWithText(AppStrings.settingsBackupRestoreBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(Part2VaultStrings.restoreExistingTitle).assertExists()
        assertTrue(restores.isEmpty(), "nothing is replaced before the officer answers")
        composeRule.onNodeWithText(AppStrings.settingsRestoreMergeBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(listOf(RestoreMode.MERGE), restores)
    }

    @Test
    fun restoreOnADeviceWithPayslipsKeepsTheShippedQuestion() {
        open(BackupContents(payslips = 1, part2Items = 0))

        composeRule.onNodeWithText(AppStrings.settingsBackupRestoreBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(AppStrings.settingsRestoreExistingTitle).assertExists()
        composeRule.onNodeWithText(AppStrings.settingsRestoreReplaceBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(listOf(RestoreMode.REPLACE), restores)
    }

    @Test
    fun restoreOnAnEmptyDeviceRestoresStraightAway() {
        open(BackupContents(payslips = 0, part2Items = 0))

        composeRule.onNodeWithText(AppStrings.settingsBackupRestoreBtn).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(listOf(RestoreMode.REPLACE), restores)
    }
}
