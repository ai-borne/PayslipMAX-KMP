package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun StagingCard(
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit,
    onCrashTestClick: (() -> Unit)? = null,
    onBackgroundCrashTestClick: (() -> Unit)? = null,
    onSimulateParserFailureClick: (() -> Unit)? = null,
    onSimulateAiFailureClick: (() -> Unit)? = null,
    onSimulateAiWaitingForWifiClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StagingDataActions(onSeedClick = onSeedClick, onClearClick = onClearClick)
        StagingDiagnosticActions(
            onCrashTestClick = onCrashTestClick,
            onBackgroundCrashTestClick = onBackgroundCrashTestClick,
            onSimulateParserFailureClick = onSimulateParserFailureClick,
            onSimulateAiFailureClick = onSimulateAiFailureClick,
            onSimulateAiWaitingForWifiClick = onSimulateAiWaitingForWifiClick,
        )
    }
}

@Composable
private fun StagingDataActions(
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    SettingsRow(
        icon = "🌱",
        title = AppStrings.settingsStagingSeedBtn,
        subtitle = AppStrings.settingsStagingDesc,
        onClick = onSeedClick,
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    SettingsRow(
        icon = "🗑️",
        title = AppStrings.settingsStagingClearBtn,
        onClick = onClearClick,
    )
}

@Composable
private fun StagingDiagnosticActions(
    onCrashTestClick: (() -> Unit)?,
    onBackgroundCrashTestClick: (() -> Unit)?,
    onSimulateParserFailureClick: (() -> Unit)?,
    onSimulateAiFailureClick: (() -> Unit)?,
    onSimulateAiWaitingForWifiClick: (() -> Unit)?,
) {
    onCrashTestClick?.let {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(icon = "💥", title = AppStrings.settingsStagingCrashTestBtn, onClick = it)
    }
    onBackgroundCrashTestClick?.let {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(icon = "⚡", title = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.settingsStagingBackgroundCrashBtn, onClick = it)
    }
    onSimulateParserFailureClick?.let {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(icon = "📋", title = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.settingsStagingSimulateParserFailureBtn, onClick = it)
    }
    onSimulateAiFailureClick?.let {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(icon = "⚠️", title = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.settingsStagingSimulateAiFailureBtn, onClick = it)
    }
    onSimulateAiWaitingForWifiClick?.let {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(icon = "📶", title = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.settingsStagingSimulateAiWaitingForWifiBtn, onClick = it)
    }
}
