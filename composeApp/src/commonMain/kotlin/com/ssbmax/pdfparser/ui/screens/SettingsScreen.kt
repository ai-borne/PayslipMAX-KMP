package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.*
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun SettingsScreen(
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("535d04") }
    val uiState by viewModel.uiState.collectAsState()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var devClicks by remember { mutableStateOf(0) }
    var devModeEnabled by remember { mutableStateOf(false) }

    SettingsContent(
        viewModel = viewModel,
        uiState = uiState,
        password = password,
        onPasswordChange = { password = it },
        onUpgradePrompt = { showUpgradeSheet = true },
        devModeEnabled = devModeEnabled,
        onHeaderClick = {
            devClicks++
            if (devClicks >= 7) {
                devModeEnabled = !devModeEnabled
                devClicks = 0
            }
        },
        modifier = modifier,
    )

    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = { showUpgradeSheet = false },
            onUnlockClick = { viewModel.setPremiumEnabled(true) },
        )
    }
}

@Composable
private fun SettingsContent(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    password: String,
    onPasswordChange: (String) -> Unit,
    onUpgradePrompt: () -> Unit,
    devModeEnabled: Boolean,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        SettingsHeader(onTitleClick = onHeaderClick)
        PrivacyCard()
        ProfileSection(viewModel = viewModel, uiState = uiState)
        PreferencesSection(viewModel = viewModel, uiState = uiState, onUpgradePrompt = onUpgradePrompt)
        SecurityBackupSection(
            viewModel = viewModel,
            uiState = uiState,
            password = password,
            onPasswordChange = onPasswordChange,
            onUpgradePrompt = onUpgradePrompt,
        )
        DeveloperSandboxSection(devModeEnabled = devModeEnabled, viewModel = viewModel)
        DangerZoneSection(viewModel = viewModel)
        SettingsHelpDocs()
    }
}

@Composable
private fun ProfileSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
) {
    SettingsCategoryHeader(title = AppStrings.settingsProfileName)
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
private fun PreferencesSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    onUpgradePrompt: () -> Unit,
) {
    SettingsCategoryHeader(title = AppStrings.navigationSettings)
    SettingsCategoryCard {
        PremiumSettingsCard(
            isPremiumEnabled = uiState.isPremiumEnabled,
            onPremiumToggle = { viewModel.setPremiumEnabled(it) },
            onUpgradePrompt = onUpgradePrompt,
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
                        onCheckedChange = { viewModel.setLocalAiEnabled(it) }
                    )
                }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ThemeSelectionCard(
            currentTheme = uiState.appTheme,
            onThemeSelect = { viewModel.setAppTheme(it) },
        )
    }
}

@Composable
private fun SecurityBackupSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    password: String,
    onPasswordChange: (String) -> Unit,
    onUpgradePrompt: () -> Unit,
) {
    SettingsCategoryHeader(title = AppStrings.settingsRowBackupLabel)
    SettingsCategoryCard {
        PasscodeSettingsCard(
            isLockEnabled = uiState.isLockEnabled,
            onLockToggle = { enabled, pin -> viewModel.setLockEnabled(enabled, pin) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
    }
}

@Composable
private fun DeveloperSandboxSection(
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
private fun DangerZoneSection(
    viewModel: PayslipViewModel,
) {
    SettingsCategoryHeader(title = AppStrings.settingsDangerZone)
    SettingsCategoryCard {
        DangerZoneCard(
            onDeleteAllClick = { viewModel.clearAllData() },
        )
    }
}

@Composable
private fun SettingsHeader(
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
