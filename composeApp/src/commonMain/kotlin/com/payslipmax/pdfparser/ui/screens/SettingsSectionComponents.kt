package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun ProfileSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
) {
    SettingsCategoryCard {
        ProfileOverridesCard(
            viewModel = viewModel,
            profileName = uiState.profileName,
            profileCda = uiState.profileCdaNumber,
            profilePan = uiState.profilePanNumber,
        )
    }
}

@Composable
fun PremiumSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    onUpgradePrompt: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    PremiumSettingsCard(
        isPremiumEnabled = uiState.isPremiumEnabled,
        onUpgradePrompt = onUpgradePrompt,
    )
    SettingsCategoryCard {
        SettingsRow(
            icon = "✨",
            title = AppStringsPremium.premiumCatalogTitle,
            subtitle = AppStringsPremium.premiumCatalogSettingsEntrySubtitle,
            onClick = { onNavigateTo(Screen.PremiumFeatures) },
        )
    }
}

@Composable
fun SecuritySection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
) {
    SettingsCategoryHeader(title = AppStrings.settingsSecurityPrivacyHeader)
    SettingsCategoryCard {
        PasscodeSettingsCard(
            isLockEnabled = uiState.isLockEnabled,
            onLockToggle = { enabled, pin -> viewModel.setLockEnabled(enabled, pin) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "🛡️",
            title = AppStrings.settingsSecurityEncryptionStatusLabel,
            subtitle = AppStrings.settingsSecurityEncryptionStatusSubtitle,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "📊",
            title = AppStrings.settingsTelemetryLabel,
            subtitle = AppStrings.settingsTelemetryDesc,
            trailingContent = {
                Switch(
                    checked = uiState.isTelemetryEnabled,
                    onCheckedChange = { viewModel.setTelemetryEnabled(it) },
                )
            },
        )
    }
}

@Composable
fun PreferencesSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
) {
    SettingsCategoryHeader(title = AppStrings.settingsThemeLabel)
    SettingsCategoryCard {
        ThemeSelectionCard(
            currentTheme = uiState.appTheme,
            onThemeSelect = { viewModel.setAppTheme(it) },
        )
    }
}

@Composable
fun DataManagementSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    password: String,
    onPasswordChange: (String) -> Unit,
    onUpgradePrompt: () -> Unit,
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
) {
    SettingsCategoryHeader(title = AppStrings.settingsDataManagementHeader)
    SettingsCategoryCard {
        BackupRestoreSettingsCard(
            password = password,
            onPasswordChange = onPasswordChange,
            payslipCount = uiState.payslips.size,
            onExportBackup = { pw, onComplete -> viewModel.exportBackup(pw, onComplete) },
            onRestore = { bytes, pw, mode, onComplete -> viewModel.importBackup(bytes, pw, mode, onComplete) },
            onPickBackup = onPickBackup,
            canBackup = viewModel.rememberHasAccess(com.payslipmax.pdfparser.subscription.FeatureGate.BACKUP_RESTORE),
            onUpgradePrompt = onUpgradePrompt,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ReparseAllCard(
            onReparseClick = { pw, onComplete -> viewModel.reparseAllPayslips(pw, onComplete) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DangerZoneCard(
            onDeleteAllClick = { viewModel.clearAllData() },
        )
    }
}

@Composable
fun HelpSupportSection(
    onNavigateTo: (Screen) -> Unit,
) {
    SettingsCategoryHeader(title = AppStrings.settingsHelpDocsHeader)
    SettingsCategoryCard {
        SettingsRow(
            icon = "❓",
            title = AppStrings.settingsHelpFaqTitle,
            onClick = { onNavigateTo(Screen.FAQ) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "📜",
            title = AppStrings.settingsHelpPrivacyTitle,
            onClick = { onNavigateTo(Screen.PrivacyPolicy) },
        )
    }
}

@Composable
fun DeveloperSandboxSection(
    devModeEnabled: Boolean,
    viewModel: PayslipViewModel,
) {
    if (devModeEnabled) {
        SettingsCategoryHeader(title = AppStrings.settingsStagingTitle)
        SettingsCategoryCard {
            StagingCard(
                onSeedClick = { viewModel.seedMockData() },
                onClearClick = { viewModel.clearAllData() },
            )
        }
    }
}

@Composable
fun SettingsHeader(
    onTitleClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AppStrings.navigationSettings,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTitleClick,
                    ),
            )
            OfflineStatusPill()
        }
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = AppStrings.settingsSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VersionFooter(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Text(
            text = AppStrings.appVersion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
