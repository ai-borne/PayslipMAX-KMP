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
import androidx.compose.ui.text.style.TextDecoration
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.GemmaModelStrings

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
            title = AppStringsPremium.proCatalogTitle,
            subtitle = AppStringsPremium.proCatalogSettingsEntrySubtitle,
            onClick = { onNavigateTo(Screen.ProFeatures) },
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
            title = "Local Encryption Status",
            subtitle = "100% Offline & Encrypted",
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        LocalGemmaAiSettingRow(viewModel, uiState)
    }
}

/**
 * This row now controls only *which source* [FinancialIntelligenceRepository] reads narrative
 * insights from (local Gemma vs. cloud Gemini) — free for everyone, no premium gate. The Tier 6
 * base model's own download progress/errors are surfaced separately by [BaseModelDownloadBanner],
 * which is unconditional and independent of this toggle.
 */
@Composable
private fun LocalGemmaAiSettingRow(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
) {
    val subtitle =
        if (uiState.isGemmaSupported) {
            GemmaModelStrings.gemmaAiSettingRowSubtitleSupported
        } else {
            uiState.gemmaSupportReason ?: GemmaModelStrings.gemmaAiSettingRowSubtitleUnsupported
        }
    SettingsRow(
        icon = "🤖",
        title = GemmaModelStrings.gemmaAiSettingRowTitle,
        subtitle = subtitle,
        trailingContent = {
            Switch(
                checked = uiState.useLocalAi && uiState.isGemmaSupported,
                onCheckedChange = { if (uiState.isGemmaSupported) viewModel.setLocalAiEnabled(it) },
                enabled = uiState.isGemmaSupported,
            )
        },
    )
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
                modifier = Modifier.clickable { onNavigateTo(Screen.PrivacyPolicy) },
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
                modifier = Modifier.clickable { onNavigateTo(Screen.PrivacyPolicy) },
            )
        }
    }
}
