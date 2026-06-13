package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PasscodeSettingsCard(
    isLockEnabled: Boolean,
    onLockToggle: (Boolean, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(AppStrings.settingsAppLockLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(AppStrings.settingsAppLockDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        }
    }

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
