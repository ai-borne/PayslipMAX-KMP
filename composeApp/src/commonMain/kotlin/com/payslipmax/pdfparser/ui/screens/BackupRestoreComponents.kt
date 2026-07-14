package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun BackupRestoreHeader(onCloseClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = AppStrings.settingsBackupHeader,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStrings.settingsBackupDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun BackupRestorePasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(AppStrings.labelPassword) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "👁️" else "🙈")
            }
        },
    )
}

@Composable
fun LocalSyncButtonsRow(
    password: String,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onStatusChange: (BackupStatus) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = {
                if (!canBackup) {
                    onLockedBackup()
                    return@Button
                }
                onBackupClick(password) { result ->
                    val statusMsg =
                        if (result.isSuccess) {
                            AppStrings.statusSyncSuccess
                        } else {
                            "${AppStrings.statusSyncFailed}${result.exceptionOrNull()?.message}"
                        }
                    onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (canBackup) AppStrings.settingsBackupLocalBtn else "🔒 ${AppStrings.settingsBackupLocalBtn}")
        }
        OutlinedButton(
            onClick = {
                onRestoreClick(password) { result ->
                    val statusMsg =
                        if (result.isSuccess) {
                            AppStrings.statusRestoreSuccess
                        } else {
                            "${AppStrings.statusRestoreFailed}${result.exceptionOrNull()?.message}"
                        }
                    onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.settingsRestoreLocalBtn)
        }
    }
}
