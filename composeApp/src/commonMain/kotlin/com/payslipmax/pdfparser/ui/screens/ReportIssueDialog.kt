package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.domain.SUPPORT_MESSAGE_MAX_CHARS
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport

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
        maxLength = SUPPORT_MESSAGE_MAX_CHARS,
        onDismiss = onDismiss,
        onConfirm = onSend,
    )
}
