package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun DangerZoneCard(
    onDeleteAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon = "⚠️",
        title = AppStrings.settingsDeleteAll,
        subtitle = AppStrings.settingsDangerZoneDesc,
        titleColor = MaterialTheme.colorScheme.error,
        onClick = { showConfirmDialog = true },
        modifier = modifier,
    )

    if (showConfirmDialog) {
        ConfirmWipeDialog(
            onConfirm = {
                onDeleteAllClick()
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false },
        )
    }
}

@Composable
private fun ConfirmWipeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.settingsDeleteConfirmTitle) },
        text = { Text(AppStrings.settingsDeleteConfirm) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(AppStrings.settingsDeleteConfirmBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btnCancel)
            }
        },
    )
}
