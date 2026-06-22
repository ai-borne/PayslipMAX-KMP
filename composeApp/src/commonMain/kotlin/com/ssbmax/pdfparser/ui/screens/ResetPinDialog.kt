package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun ResetPinDialog(
    onDismiss: () -> Unit,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onResetPin: (ByteArray, String, String, (Result<Unit>) -> Unit) -> Unit,
) {
    var selectedPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedPdfName by remember { mutableStateOf("") }
    var pdfPassword by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }
    var isResetting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.lockScreenResetDialogTitle) },
        text = {
            ResetPinDialogContent(
                selectedPdfName = selectedPdfName,
                pdfPassword = pdfPassword,
                resetError = resetError,
                onPdfPasswordChange = { pdfPassword = it },
                onSelectPdfClick = {
                    onPickPdf { bytes, name ->
                        selectedPdfBytes = bytes
                        selectedPdfName = name
                        resetError = null
                    }
                }
            )
        },
        confirmButton = {
            Button(
                enabled = selectedPdfBytes != null && pdfPassword.isNotEmpty() && !isResetting,
                onClick = {
                    val bytes = selectedPdfBytes ?: return@Button
                    handleReset(bytes, pdfPassword, selectedPdfName, onResetPin, { isResetting = it }, { resetError = it }, onDismiss)
                }
            ) { Text("Verify & Reset") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.btnCancel) }
        }
    )
}

@Composable
private fun ResetPinDialogContent(
    selectedPdfName: String,
    pdfPassword: String,
    resetError: String?,
    onPdfPasswordChange: (String) -> Unit,
    onSelectPdfClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = AppStrings.lockScreenResetDialogDesc,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onSelectPdfClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedPdfName.isNotEmpty()) selectedPdfName else AppStrings.labelSelectPdf)
        }
        OutlinedTextField(
            value = pdfPassword,
            onValueChange = onPdfPasswordChange,
            label = { Text(AppStrings.labelPassword) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (resetError != null) {
            Text(
                text = resetError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun handleReset(
    bytes: ByteArray,
    password: String,
    filename: String,
    onResetPin: (ByteArray, String, String, (Result<Unit>) -> Unit) -> Unit,
    setResetting: (Boolean) -> Unit,
    setResetError: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    setResetting(true)
    setResetError(null)
    onResetPin(bytes, password, filename) { result ->
        setResetting(false)
        if (result.isSuccess) {
            onDismiss()
        } else {
            val msg = result.exceptionOrNull()?.message
            val errorMsg = if (msg?.contains("PAN", ignoreCase = true) == true) {
                AppStrings.lockScreenResetErrorMismatch
            } else {
                msg ?: "Failed to reset passcode."
            }
            setResetError(errorMsg)
        }
    }
}
