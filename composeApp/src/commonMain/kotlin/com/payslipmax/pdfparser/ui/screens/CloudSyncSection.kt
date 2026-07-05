package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.CloudSyncStrings

@Composable
fun CloudSyncSection(
    userId: String,
    onUserIdChange: (String) -> Unit,
    authToken: String,
    onAuthTokenChange: (String) -> Unit,
    onCloudBackupClick: () -> Unit,
    onCloudRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text(
            text = CloudSyncStrings.cloudSyncHeader,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        CloudSyncInputs(
            userId = userId,
            onUserIdChange = onUserIdChange,
            authToken = authToken,
            onAuthTokenChange = onAuthTokenChange,
        )

        CloudSyncButtons(
            onCloudBackupClick = onCloudBackupClick,
            onCloudRestoreClick = onCloudRestoreClick,
            enabled = userId.isNotBlank() && authToken.isNotBlank(),
        )
    }
}

@Composable
private fun CloudSyncInputs(
    userId: String,
    onUserIdChange: (String) -> Unit,
    authToken: String,
    onAuthTokenChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        OutlinedTextField(
            value = userId,
            onValueChange = onUserIdChange,
            label = { Text(CloudSyncStrings.cloudSyncUidLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = authToken,
            onValueChange = onAuthTokenChange,
            label = { Text(CloudSyncStrings.cloudSyncTokenLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun CloudSyncButtons(
    onCloudBackupClick: () -> Unit,
    onCloudRestoreClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = onCloudBackupClick,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(CloudSyncStrings.cloudSyncBackupBtn)
        }

        OutlinedButton(
            onClick = onCloudRestoreClick,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(CloudSyncStrings.cloudSyncRestoreBtn)
        }
    }
}
