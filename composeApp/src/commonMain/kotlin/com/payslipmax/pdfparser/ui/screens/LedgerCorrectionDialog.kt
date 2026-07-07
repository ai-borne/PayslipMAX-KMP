package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.payslipmax.pdfparser.ui.theme.AppColors
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Phase 7 — inline correction dialog supporting edits, additions, and deletions of ledger fields.
 */
@Composable
internal fun LedgerCorrectionDialog(
    line: LedgerLine?,
    isEarning: Boolean,
    reasons: List<String> = emptyList(),
    diagnosticHint: String? = null,
    isDeletedInitial: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (fieldKey: String, codeHead: String, amount: Double, isDelete: Boolean) -> Unit,
) {
    var isDeleteSelected by remember { mutableStateOf(isDeletedInitial) }
    var codeHeadInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf(if (line != null) formatEditable(line.amount) else "") }
    val parsedAmount = amountInput.trim().replace(",", "").toDoubleOrNull()
    val isAmountValid = parsedAmount != null && parsedAmount >= 0.0
    val isValid = if (line != null && isDeleteSelected) true else (isAmountValid && (line != null || codeHeadInput.isNotBlank()))
    val dialogTitle =
        if (line != null) {
            "${AppStrings.correctionDialogTitle} · ${line.code}"
        } else if (isEarning) {
            AppStrings.correctionDialogAddEarningTitle
        } else {
            AppStrings.correctionDialogAddDeductionTitle
        }
    val confirmText = if (line == null) AppStrings.correctionDialogSaveDraft else AppStrings.correctionSave

    RenderDialog(
        title = dialogTitle,
        confirmText = confirmText,
        isValid = isValid,
        onDismiss = onDismiss,
        onConfirmClick = { performConfirm(line, isEarning, isDeleteSelected, codeHeadInput, parsedAmount, onConfirm) },
    ) {
        DialogBody(
            line = line, isEarning = isEarning, reasons = reasons, diagnosticHint = diagnosticHint,
            isDeleteSelected = isDeleteSelected, onDeleteSelectedChange = { isDeleteSelected = it },
            codeHeadInput = codeHeadInput, onCodeHeadChange = { codeHeadInput = it },
            amountInput = amountInput, onAmountChange = { amountInput = it }, isAmountValid = isAmountValid,
        )
    }
}

@Composable
private fun RenderDialog(
    title: String,
    confirmText: String,
    isValid: Boolean,
    onDismiss: () -> Unit,
    onConfirmClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirmClick, enabled = isValid) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = AppStrings.correctionCancel)
            }
        },
    )
}

private fun performConfirm(
    line: LedgerLine?,
    isEarning: Boolean,
    isDeleteSelected: Boolean,
    codeHeadInput: String,
    parsedAmount: Double?,
    onConfirm: (fieldKey: String, codeHead: String, amount: Double, isDelete: Boolean) -> Unit,
) {
    if (line != null) {
        if (isDeleteSelected) {
            onConfirm(line.fieldKey, line.code, 0.0, true)
        } else {
            parsedAmount?.let { onConfirm(line.fieldKey, line.code, it, false) }
        }
    } else {
        val finalCodeHead = codeHeadInput.trim()
        val finalFieldKey =
            if (isEarning) {
                com.payslipmax.pdfparser.parser.PayslipPatternConfig.creditKeysMapping[finalCodeHead]
            } else {
                com.payslipmax.pdfparser.parser.PayslipPatternConfig.debitKeysMapping[finalCodeHead]
            } ?: finalCodeHead
        parsedAmount?.let { onConfirm(finalFieldKey, finalCodeHead, it, false) }
    }
}

@Composable
private fun DialogBody(
    line: LedgerLine?,
    isEarning: Boolean,
    reasons: List<String>,
    diagnosticHint: String?,
    isDeleteSelected: Boolean,
    onDeleteSelectedChange: (Boolean) -> Unit,
    codeHeadInput: String,
    onCodeHeadChange: (String) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    isAmountValid: Boolean,
) {
    Column {
        if (line == null) {
            Text(text = AppStrings.correctionDialogHint)
        }
        ReviewReasonsList(reasons)
        DiagnosticHintText(diagnosticHint)
        Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))

        if (line != null) {
            ActionSelector(isDeleteSelected = isDeleteSelected, onToggle = onDeleteSelectedChange)
        }

        if (isDeleteSelected) {
            DeleteWarningText()
        } else {
            CorrectionForm(
                isAdding = line == null,
                codeHeadInput = codeHeadInput,
                onCodeHeadChange = onCodeHeadChange,
                amountInput = amountInput,
                onAmountChange = onAmountChange,
                isAmountValid = isAmountValid,
                originalAmount = line?.amount,
            )
        }
    }
}

@Composable
private fun ActionSelector(
    isDeleteSelected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = AppDimensions.SpacingMedium),
    ) {
        RadioButton(selected = !isDeleteSelected, onClick = { onToggle(false) })
        Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
        Text(AppStrings.correctionDialogEditOption, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))
        RadioButton(selected = isDeleteSelected, onClick = { onToggle(true) })
        Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
        Text(AppStrings.correctionDialogDeleteOption, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CorrectionForm(
    isAdding: Boolean,
    codeHeadInput: String,
    onCodeHeadChange: (String) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    isAmountValid: Boolean,
    originalAmount: Double?,
) {
    Column {
        if (isAdding) {
            OutlinedTextField(
                value = codeHeadInput,
                onValueChange = onCodeHeadChange,
                singleLine = true,
                label = { Text(AppStrings.correctionDialogCodeHeadLabel) },
                placeholder = { Text(AppStrings.correctionDialogCustomCodeHeadPlaceholder) },
                modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.SpacingSmall),
            )
        }
        OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            singleLine = true,
            isError = !isAmountValid && amountInput.isNotEmpty(),
            label = { Text(AppStrings.correctionDialogAmountLabel) },
            supportingText = {
                if (!isAmountValid && amountInput.isNotEmpty()) {
                    Text(AppStrings.correctionInvalidAmount)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingSmall),
        )
        if (originalAmount != null) {
            Text(
                text = "${AppStrings.correctionDialogOriginalAmountLabel}${formatEditable(originalAmount)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = AppDimensions.SpacingSmall),
            )
        }
    }
}

@Composable
private fun DeleteWarningText() {
    Text(
        text = AppStrings.correctionDialogDeleteWarning,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = AppDimensions.SpacingSmall),
    )
}

@Composable
private fun ReviewReasonsList(reasons: List<String>) {
    if (reasons.isEmpty()) return
    Text(
        text = AppStrings.correctionReasonsHeading,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = AppDimensions.SpacingSmall),
    )
    reasons.forEach { reason ->
        Text(text = "• ${formatReviewReason(reason)}", style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatReviewReason(reason: String): String {
    return when {
        reason.startsWith("Net residual") || reason.contains("NET_TOLERANCE") -> {
            "The Take-Home pay does not match the difference between total earnings and deductions."
        }
        reason.startsWith("Low confidence fields") -> {
            val fields = reason.substringAfter(": ").trim()
            "Verify these low confidence entries: $fields"
        }
        reason.startsWith("Missing mandatory credits") -> {
            val fields = reason.substringAfter(": ").trim()
            "Expected earnings entries not detected: $fields"
        }
        reason.startsWith("Missing mandatory debits") -> {
            val fields = reason.substringAfter(": ").trim()
            "Expected deductions entries not detected: $fields"
        }
        reason.startsWith("Schema validation failed") || reason.contains("mismatch") -> {
            "The sum of items does not match the printed Gross Pay or Total Deductions."
        }
        else -> reason
    }
}

@Composable
private fun DiagnosticHintText(diagnosticHint: String?) {
    if (diagnosticHint == null) return
    Text(
        text = AppStrings.correctionDiagnosticHintHeading,
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.AiInferred,
        modifier = Modifier.padding(top = AppDimensions.SpacingSmall),
    )
    Text(text = diagnosticHint, style = MaterialTheme.typography.bodySmall)
}

private fun formatEditable(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}
