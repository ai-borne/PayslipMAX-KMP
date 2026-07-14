package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

data class BackupStatus(
    val message: String,
    val isSuccess: Boolean,
)

@Composable
fun UniversalBackupSection(
    password: String,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var importText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(
            text = AppStrings.settingsBackupCrossPlatform,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        ExportImportButtonsRow(
            canBackup = canBackup,
            isImporting = isImporting,
            onLockedBackup = onLockedBackup,
            onExportClick = onExportClick,
            onToggleImport = { isImporting = !isImporting },
        )
        if (isImporting) {
            ImportControls(
                importText = importText,
                onImportTextChange = { importText = it },
                onImportConfirm = {
                    onImportClick(importText)
                    importText = ""
                    isImporting = false
                },
            )
        }
    }
}

@Composable
private fun ExportImportButtonsRow(
    canBackup: Boolean,
    isImporting: Boolean,
    onLockedBackup: () -> Unit,
    onExportClick: () -> Unit,
    onToggleImport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = { if (canBackup) onExportClick() else onLockedBackup() },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (canBackup) AppStrings.settingsBackupExportBtn else "🔒 ${AppStrings.settingsBackupExportBtn}")
        }
        OutlinedButton(onClick = onToggleImport, modifier = Modifier.weight(1f)) {
            Text(if (isImporting) AppStrings.settingsBackupCancelImportBtn else AppStrings.settingsBackupImportBtn)
        }
    }
}

@Composable
private fun ImportControls(
    importText: String,
    onImportTextChange: (String) -> Unit,
    onImportConfirm: () -> Unit,
) {
    OutlinedTextField(
        value = importText,
        onValueChange = onImportTextChange,
        label = { Text(AppStrings.settingsBackupPasteLabel) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 4,
    )
    Button(
        onClick = onImportConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = importText.isNotBlank(),
    ) {
        Text(AppStrings.settingsBackupDecryptBtn)
    }
}

@Composable
fun StatusMessage(status: BackupStatus) {
    Text(
        text = status.message,
        style = MaterialTheme.typography.labelSmall,
        color = if (status.isSuccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny),
    )
}
