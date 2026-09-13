package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.isFreeLaunchModePlatform
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun AccountSubscriptionSection(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    onUpgradePrompt: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    val premiumPrice by viewModel.premiumPriceState.collectAsState()

    SettingsCategoryCard {
        ProfileOverridesCard(
            viewModel = viewModel,
            profileName = uiState.profileName,
            profileCda = uiState.profileCdaNumber,
            profilePan = uiState.profilePanNumber,
        )
        if (!isFreeLaunchModePlatform()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PremiumSettingsCardContentRow(
                isPremiumEnabled = uiState.isPremiumEnabled,
                onUpgradePrompt = onUpgradePrompt,
                price = premiumPrice,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "✨",
            title = AppStringsPremium.premiumCatalogTitleDisplay,
            subtitle = AppStringsPremium.premiumCatalogSettingsEntrySubtitleDisplay,
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRow(
            icon = "🛠️",
            title = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.settingsReportIssueTitle,
            subtitle = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.settingsReportIssueSubtitle,
            onClick = {
                val sysInfo = com.payslipmax.pdfparser.platform.getPlatformSystemInfo()
                val anonId = com.payslipmax.pdfparser.telemetry.InstallationIdManager().getOrCreateInstallationId()
                val report =
                    com.payslipmax.pdfparser.telemetry.DiagnosticReportGenerator.generateReport(
                        appVersion = com.payslipmax.pdfparser.platform.platformAppVersion(),
                        installationId = anonId,
                        osVersion = sysInfo.osVersion,
                        deviceModel = sysInfo.deviceModel,
                        cpuArch = sysInfo.cpuArchitecture,
                        lastScreen = "Settings",
                        lastOperation = "User Support Request",
                    )
                com.payslipmax.pdfparser.utils.shareText(
                    text = report,
                    title = com.payslipmax.pdfparser.ui.theme.AppStringsSupport.reportIssueShareTitle,
                )
            },
        )
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
    val anonId =
        remember {
            com.payslipmax.pdfparser.telemetry.InstallationIdManager().getOrCreateInstallationId()
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text(
            text = AppStrings.appVersion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = "${com.payslipmax.pdfparser.ui.theme.AppStringsSupport.installationIdPrefix}$anonId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
