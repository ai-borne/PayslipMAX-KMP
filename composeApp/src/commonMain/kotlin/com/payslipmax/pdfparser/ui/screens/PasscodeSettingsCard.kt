package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun PasscodeSettingsCard(
    isLockEnabled: Boolean,
    onLockToggle: (Boolean, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    val statusText =
        if (isLockEnabled) {
            AppStrings.settingsStatusEnabled
        } else {
            AppStrings.settingsStatusDisabled
        }

    SettingsRow(
        icon = "🔒",
        title = AppStrings.settingsRowPasscodeLabel,
        subtitle = statusText,
        trailingContent = {
            Switch(
                checked = isLockEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        showPinDialog = true
                    } else {
                        onLockToggle(false, "")
                    }
                },
            )
        },
        modifier = modifier,
    )

    if (showPinDialog) {
        PasscodeInputDialog(
            pinText = pinText,
            onPinChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinText = it },
            onConfirm = {
                onLockToggle(true, pinText)
                pinText = ""
                showPinDialog = false
            },
            onDismiss = {
                pinText = ""
                showPinDialog = false
            },
        )
    }
}

@Composable
private fun PasscodeInputDialog(
    pinText: String,
    onPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.settingsSetPasscodeTitle) },
        text = {
            OutlinedTextField(
                value = pinText,
                onValueChange = onPinChange,
                label = { Text(AppStrings.settingsSetPasscodeLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = pinText.length == 4,
            ) {
                Text(AppStrings.settingsSetPasscodeConfirmBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btnCancel)
            }
        },
    )
}
