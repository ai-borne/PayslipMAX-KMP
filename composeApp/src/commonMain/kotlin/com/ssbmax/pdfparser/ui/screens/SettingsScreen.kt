package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun SettingsScreen(
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("535d04") }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsHeader()
        PrivacyCard()
        BackupRestoreSettingsCard(
            password = password,
            onPasswordChange = { password = it },
            onBackupClick = { pw, onComplete -> viewModel.backupDatabase(pw, onComplete) },
            onRestoreClick = { pw, onComplete -> viewModel.restoreDatabase(pw, onComplete) },
        )
        StagingCard(
            onSeedClick = { viewModel.seedMockData() },
            onClearClick = { viewModel.clearAllData() },
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = AppStrings.navigationSettings,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Manage your data, sync backups, and configure sandbox options",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🛡️",
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(
                    text = "100% Offline & Secure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "All payslip decryption and parsing happens locally on your device. Your data never leaves your control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackupRestoreSettingsCard(
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

@Composable
private fun StagingCard(
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Staging & Testing Sandbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Load simulated Army Officer records (2022-2025) to evaluate the analytical tools immediately.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            StagingButtonsRow(onSeedClick, onClearClick)
        }
    }
}

@Composable
private fun StagingButtonsRow(
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onSeedClick,
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
        ) {
            Text("Seed Staging Data")
        }
        OutlinedButton(
            onClick = onClearClick,
            modifier = Modifier.weight(1f),
        ) {
            Text("Clear All")
        }
    }
}
