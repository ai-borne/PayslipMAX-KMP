package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/** What a backup or restore would meet on this device: payslips and Part II items (drafts and vault orders; pages belong to an order). */
class BackupContents(
    val payslips: Int,
    val part2Items: Int,
) {
    val isEmpty: Boolean get() = payslips + part2Items == 0
}

/** Why a backup cannot start (no password, or nothing to back up), or null when it can. */
internal fun backupRefusal(
    password: String,
    contents: BackupContents,
): BackupStatus? =
    when {
        password.isBlank() -> BackupStatus(AppStrings.settingsBackupPasswordRequired, isSuccess = false)
        contents.isEmpty -> BackupStatus(AppStrings.statusNoPayslipsToBackup, isSuccess = false)
        else -> null
    }

/** The line shown once a backup has been handed to the share sheet: the shipped payslip wording, plus the Part II items when there are any. */
internal fun backupDoneStatus(contents: BackupContents): BackupStatus {
    val payslips = "${contents.payslips} ${AppStrings.labelPayslipsBackedUp}"
    val items = Part2VaultStrings.backedUpItems(contents.part2Items)
    val message =
        when {
            contents.part2Items == 0 -> payslips
            contents.payslips == 0 -> items
            else -> "$payslips, $items"
        }
    return BackupStatus(message, isSuccess = true)
}

/** Whether a restore must ask Replace or Merge first: something is already on the device. */
internal fun restoreNeedsChoice(contents: BackupContents): Boolean = !contents.isEmpty

/** The title of the Replace/Merge question: payslips when there are any (the shipped wording), otherwise Part II items. */
internal fun restoreChoiceTitle(contents: BackupContents): String =
    if (contents.payslips > 0) AppStrings.settingsRestoreExistingTitle else Part2VaultStrings.restoreExistingTitle
