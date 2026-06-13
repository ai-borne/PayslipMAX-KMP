package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun UniversalBackupSection(
    password: String,
    onExportClick: () -> Unit,
    onImportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var importText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(
            text = AppStrings.settingsBackupCrossPlatform,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onExportClick, modifier = Modifier.weight(1f)) {
                Text(AppStrings.settingsBackupExportBtn)
            }
            OutlinedButton(onClick = { isImporting = !isImporting }, modifier = Modifier.weight(1f)) {
                Text(
                    if (isImporting) {
                        AppStrings.settingsBackupCancelImportBtn
                    } else {
                        AppStrings.settingsBackupImportBtn
                    },
                )
            }
        }
        if (isImporting) {
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text(AppStrings.settingsBackupPasteLabel) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
            Button(
                onClick = {
                    onImportClick(importText)
                    importText = ""
                    isImporting = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = importText.isNotBlank(),
            ) {
                Text(AppStrings.settingsBackupDecryptBtn)
            }
        }
    }
}

@Composable
fun StatusMessage(message: String) {
    val isSuccess =
        message.contains("Success") || message.contains("Completed") ||
            message.contains("Complete") || message.contains("Copied")
    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = if (isSuccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}
