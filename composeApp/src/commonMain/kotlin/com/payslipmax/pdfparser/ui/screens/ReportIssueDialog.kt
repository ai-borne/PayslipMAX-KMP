package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport

/** Keeps the pre-filled `mailto:` URL comfortably within what mail apps accept. */
private const val MAX_DESCRIPTION_LENGTH = 1000

@Composable
fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onSend: (description: String) -> Unit,
) {
    SupportMessageDialog(
        title = AppStringsSupport.reportIssueDialogTitle,
        notice = AppStringsSupport.reportIssuePrivacyNotice,
        fieldLabel = AppStringsSupport.reportIssueDescriptionLabel,
        confirmLabel = AppStringsSupport.reportIssueSendBtn,
        maxLength = MAX_DESCRIPTION_LENGTH,
        onDismiss = onDismiss,
        onConfirm = onSend,
    )
}
