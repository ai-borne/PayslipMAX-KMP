package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ImportScreen(
    viewModel: PayslipViewModel,
    onPickPdfTrigger: (password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var password by remember { mutableStateOf("535d04") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        HeaderSection()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ImportCard(
            password = password,
            onPasswordChange = { password = it },
            onPickClick = { onPickPdfTrigger(password) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BackupRestoreCard(
            password = password,
            onBackupClick = { pw, onComplete -> viewModel.backupDatabase(pw, onComplete) },
            onRestoreClick = { pw, onComplete -> viewModel.restoreDatabase(pw, onComplete) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MockSeederCard(
            onSeedClick = { viewModel.seedMockData() },
            onClearClick = { viewModel.clearAllData() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        StatusSection(
            isLoading = uiState.isLoading,
            error = uiState.error,
            success = uiState.importSuccess,
            onClearError = { viewModel.clearError() }
        )
    }
}

@Composable
private fun HeaderSection() {
    Text(
        text = AppStrings.uploadHeader,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = AppStrings.uploadDesc,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ImportCard(
    password: String,
    onPasswordChange: (String) -> Unit,
    onPickClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(AppStrings.labelPassword) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPickClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(AppStrings.labelSelectPdf, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MockSeederCard(
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Manual Staging Options",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Load simulated Army Officer records (2022-2025) to test the analytical charts instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSeedClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Seed Staging Data")
                }
                OutlinedButton(
                    onClick = onClearClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear All")
                }
            }
        }
    }
}

@Composable
private fun StatusSection(
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onClearError: () -> Unit
) {
    if (isLoading) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(AppStrings.loaderDecrypt, style = MaterialTheme.typography.bodyMedium)
    }
    error?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClearError) {
            Text("Dismiss")
        }
    }
    if (success) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Payslip Imported Successfully!", color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun BackupRestoreCard(
    password: String,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit
) {
    var status by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Secure Personal Cloud Sync",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AES-256 encrypts your data locally and writes it to iCloud (iOS) or secure app storage synced with Google Drive (Android).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onBackupClick(password) { result ->
                            status = if (result.isSuccess) "Sync Completed!" else "Sync Failed: ${result.exceptionOrNull()?.message}"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sync Backup")
                }
                OutlinedButton(
                    onClick = {
                        onRestoreClick(password) { result ->
                            status = if (result.isSuccess) "Restore Completed!" else "Restore Failed: ${result.exceptionOrNull()?.message}"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore")
                }
            }
            status?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (it.contains("Success") || it.contains("Complete")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
