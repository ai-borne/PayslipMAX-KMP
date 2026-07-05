package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

/**
 * Phase 5 — inline correction dialog for a single low-confidence ledger field. Pre-fills the parsed
 * value; on save it parses the entered amount and reports it back keyed by [LedgerLine.fieldKey] so the
 * caller can persist an encrypted, merged-on-read correction. Strings/dimensions are themed (no literals).
 * [reasons] are the payslip-level (not field-specific) causes of [com.ssbmax.pdfparser.domain.ParsedPayslip.needsReview],
 * surfaced here since opening this dialog is the moment a user is already looking at a flagged field.
 */
@Composable
internal fun LedgerCorrectionDialog(
    line: LedgerLine,
    reasons: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (fieldKey: String, newValue: Double) -> Unit,
) {
    var input by remember {
        mutableStateOf(TextFieldValue(formatEditable(line.amount)))
    }
    val parsed = input.text.trim().replace(",", "").toDoubleOrNull()
    val isValid = parsed != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "${AppStrings.correctionDialogTitle} · ${line.code}") },
        text = {
            Column {
                Text(text = AppStrings.correctionDialogHint)
                ReviewReasonsList(reasons)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    isError = !isValid,
                    label = { Text(text = AppStrings.correctionFieldLabel) },
                    supportingText = {
                        if (!isValid) Text(text = AppStrings.correctionInvalidAmount)
                    },
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = AppDimensions.SpacingMedium),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(line.fieldKey, it) } },
                enabled = isValid,
            ) {
                Text(text = AppStrings.correctionSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = AppStrings.correctionCancel)
            }
        },
    )
}

/** Payslip-level reasons [com.ssbmax.pdfparser.domain.ParsedPayslip.needsReview] fired, shown above the correction field. */
@Composable
private fun ReviewReasonsList(reasons: List<String>) {
    if (reasons.isEmpty()) return
    Text(
        text = AppStrings.correctionReasonsHeading,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = AppDimensions.SpacingSmall),
    )
    reasons.forEach { reason ->
        Text(text = "• $reason", style = MaterialTheme.typography.bodySmall)
    }
}

/** Renders the stored double without a trailing ".0" so whole-rupee amounts pre-fill cleanly. */
private fun formatEditable(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}
