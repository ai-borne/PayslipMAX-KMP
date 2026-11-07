package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis

/** D12: no Cancel for a backup or restore, so the sheet cannot be closed (swipe, scrim or Back) while one is running. */
internal fun BackupUiState.sheetMayHide(): Boolean = this !is BackupUiState.Working

/** The password fields and both buttons are live unless an operation is running (a second tap would start a second operation). */
internal fun BackupUiState.formIsLive(): Boolean = this !is BackupUiState.Working

/** Back Up needs the password typed twice the same: a typo would make a backup that can never be opened. */
internal fun passwordsMatch(
    password: CharSequence,
    confirm: CharSequence,
): Boolean = password.length == confirm.length && password.indices.all { password[it] == confirm[it] }

/** D11: Replace deletes what the device holds and restores less when rows were skipped, so it is asked a second time. Merge deletes nothing. */
internal fun replaceNeedsSecondConfirm(
    preview: RestorePreview,
    mode: RestoreMode,
): Boolean = mode == RestoreMode.REPLACE && preview.skipped.total > 0

/** The day [epochMillis] fell on as the officer lived it (this device's offset), written DD/MM/YYYY like the Orders Bank's backup line. */
internal fun backupDateText(epochMillis: Long): String = PreClaimDates.format(PreClaimDates.fromUtcMillis(epochMillis + localUtcOffsetMillis(epochMillis)))
