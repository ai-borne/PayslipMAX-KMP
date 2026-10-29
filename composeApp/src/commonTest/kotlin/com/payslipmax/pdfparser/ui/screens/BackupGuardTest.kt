package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 56): an officer who has only used Part II (drafts, scanned orders) has no payslips, and Back Up said "No payslips to
 * back up yet" while Restore replaced the vault without asking. What counts as "something to back up" is one rule, kept here so the
 * screen cannot drift from it: a payslip, or a Part II draft or order.
 */
class BackupGuardTest {
    @Test
    fun aBlankPasswordIsRefusedFirstWhateverTheDeviceHolds() {
        val refusal = assertNotNull(backupRefusal(" ", BackupContents(payslips = 0, part2Items = 0)))
        assertEquals(AppStrings.settingsBackupPasswordRequired, refusal.message)
        assertFalse(refusal.isSuccess)
    }

    @Test
    fun nothingAtAllIsRefusedWithALineThatDoesNotNamePayslipsOnly() {
        val refusal = assertNotNull(backupRefusal("pw", BackupContents(0, 0)))
        assertEquals(AppStrings.statusNothingToBackup, refusal.message)
        assertEquals("Nothing to back up yet.", refusal.message)
        assertFalse("payslip" in refusal.message.lowercase(), "item 69(b): drafts, orders and scanned pages are backed up too")
        assertFalse(refusal.isSuccess)
    }

    @Test
    fun payslipsAloneOrPartTwoItemsAloneOrBothAreBackedUp() {
        assertNull(backupRefusal("pw", BackupContents(2, 0)))
        assertNull(backupRefusal("pw", BackupContents(0, 3)), "the case item 56 found: Part II data and no payslips")
        assertNull(backupRefusal("pw", BackupContents(2, 3)))
    }

    @Test
    fun theDoneLineKeepsTheShippedPayslipWordingAndNamesPartTwoItemsWhenThereAreAny() {
        assertEquals("2 ${AppStrings.labelPayslipsBackedUp}", backupDoneStatus(BackupContents(2, 0)).message)
        assertEquals("2 ${AppStrings.labelPayslipsBackedUp}, ${Part2VaultStrings.backedUpItems(3)}", backupDoneStatus(BackupContents(2, 3)).message)
        assertEquals(Part2VaultStrings.backedUpItems(1), backupDoneStatus(BackupContents(0, 1)).message)
        assertTrue(backupDoneStatus(BackupContents(0, 1)).isSuccess)
    }

    @Test
    fun partTwoItemsAreCountedInWordsAsOneOrMany() {
        assertEquals("1 Part II item backed up", Part2VaultStrings.backedUpItems(1))
        assertEquals("3 Part II items backed up", Part2VaultStrings.backedUpItems(3))
    }

    @Test
    fun aRestoreAsksReplaceOrMergeWhenAnythingIsAlreadyOnTheDevice() {
        assertFalse(restoreNeedsChoice(BackupContents(0, 0)), "a fresh device restores straight away")
        assertTrue(restoreNeedsChoice(BackupContents(1, 0)))
        assertTrue(restoreNeedsChoice(BackupContents(0, 1)), "Part II only: replacing it silently would lose the officer's vault")
    }

    @Test
    fun theQuestionNamesWhatIsThere() {
        assertEquals(AppStrings.settingsRestoreExistingTitle, restoreChoiceTitle(BackupContents(1, 0)))
        assertEquals(AppStrings.settingsRestoreExistingTitle, restoreChoiceTitle(BackupContents(1, 4)))
        assertEquals(Part2VaultStrings.restoreExistingTitle, restoreChoiceTitle(BackupContents(0, 4)))
    }
}
