package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun BackupRestoreSettingsCard(
    password: String,
    onPasswordChange: (String) -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
) {
    var status by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Personal Cloud Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Backup is locally AES-256 encrypted using your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(AppStrings.labelPassword) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            BackupRestoreButtonsRow(password, onBackupClick, onRestoreClick) { status = it }
            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (it.contains("Success") || it.contains("Complete")) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }
        }
    }
}

@Composable
private fun BackupRestoreButtonsRow(
    password: String,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onStatusChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                onBackupClick(password) { result ->
                    onStatusChange(
                        if (result.isSuccess) {
                            "Sync Completed!"
                        } else {
                            "Sync Failed: ${result.exceptionOrNull()?.message}"
                        },
                    )
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("Sync Backup")
        }
        OutlinedButton(
            onClick = {
                onRestoreClick(password) { result ->
                    onStatusChange(
                        if (result.isSuccess) {
                            "Restore Completed!"
                        } else {
                            "Restore Failed: ${result.exceptionOrNull()?.message}"
                        },
                    )
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("Restore")
        }
    }
}
