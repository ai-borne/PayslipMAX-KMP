package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.Screen
import com.ssbmax.pdfparser.ui.*
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

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
) {
    PremiumSettingsCard(
        isPremiumEnabled = uiState.isPremiumEnabled,
        onPremiumToggle = { viewModel.setPremiumEnabled(it) },
        onUpgradePrompt = onUpgradePrompt,
    )
}

@Composable
fun SecuritySection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
) {
    SettingsCategoryHeader(title = AppStrings.settingsHelpPrivacyTitle)
    SettingsCategoryCard {
        PasscodeSettingsCard(
            isLockEnabled = uiState.isLockEnabled,
            onLockToggle = { enabled, pin -> viewModel.setLockEnabled(enabled, pin) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "🛡️",
            title = "Local Encryption Status",
            subtitle = "100% Offline & Encrypted",
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
        if (uiState.isPremiumEnabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsRow(
                icon = "🤖",
                title = "Use Local Gemma AI Model",
                subtitle = "Runs 100% offline on-device to protect privacy and battery.",
                trailingContent = {
                    Switch(
                        checked = uiState.useLocalAi,
                        onCheckedChange = { viewModel.setLocalAiEnabled(it) },
                    )
                },
            )
        }
    }
}

@Composable
fun DataManagementSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    password: String,
    onPasswordChange: (String) -> Unit,
    onUpgradePrompt: () -> Unit,
) {
    SettingsCategoryHeader(title = "Data Management")
    SettingsCategoryCard {
        BackupRestoreSettingsCard(
            password = password,
            onPasswordChange = onPasswordChange,
            onBackupClick = { pw, onComplete -> viewModel.backupDatabase(pw, onComplete) },
            onRestoreClick = { pw, onComplete -> viewModel.restoreDatabase(pw, onComplete) },
            onExportBackup = { pw, onComplete -> viewModel.exportBackup(pw, onComplete) },
            onImportBackup = { bytes, pw, onComplete -> viewModel.importBackup(bytes, pw, onComplete) },
            onCloudBackupClick = { uid, token, pw, onComplete -> viewModel.backupToCloud(uid, token, pw, onComplete) },
            onCloudRestoreClick = { uid, token, pw, onComplete -> viewModel.restoreFromCloud(uid, token, pw, onComplete) },
            isPremiumEnabled = uiState.isPremiumEnabled,
            onUpgradePrompt = onUpgradePrompt,
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
            onClick = { onNavigateTo(Screen.HelpLegal) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "📜",
            title = AppStrings.settingsHelpPrivacyTitle,
            onClick = { onNavigateTo(Screen.HelpLegal) },
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
    onNavigateTo: (Screen) -> Unit,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AppStrings.settingsHelpPrivacyTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onNavigateTo(Screen.HelpLegal) },
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = AppStrings.settingsHelpAiTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onNavigateTo(Screen.HelpLegal) },
            )
        }
    }
}
