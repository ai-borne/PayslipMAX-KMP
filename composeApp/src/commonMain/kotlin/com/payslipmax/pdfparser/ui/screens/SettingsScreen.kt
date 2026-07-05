package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun SettingsScreen(
    viewModel: PayslipViewModel,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
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
        onNavigateTo = onNavigateTo,
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
    onNavigateTo: (Screen) -> Unit,
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
        PrimarySettingsGroup(viewModel, uiState, onUpgradePrompt)
        SecondarySettingsGroup(
            viewModel = viewModel,
            uiState = uiState,
            password = password,
            onPasswordChange = onPasswordChange,
            onUpgradePrompt = onUpgradePrompt,
            onNavigateTo = onNavigateTo,
            devModeEnabled = devModeEnabled,
        )
    }
}

@Composable
private fun PrimarySettingsGroup(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    onUpgradePrompt: () -> Unit,
) {
    // 1. Account & Subscription
    SettingsCategoryHeader(title = AppStrings.settingsAccountSubscriptionHeader)

    // Profile Section
    ProfileSection(viewModel = viewModel, uiState = uiState)

    // PayslipMax Pro Section
    PremiumSection(viewModel = viewModel, uiState = uiState, onUpgradePrompt = onUpgradePrompt)

    // 2. Security & Privacy
    SecuritySection(viewModel = viewModel, uiState = uiState)

    // 3. Preferences
    PreferencesSection(viewModel = viewModel, uiState = uiState)
}

@Composable
private fun SecondarySettingsGroup(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    password: String,
    onPasswordChange: (String) -> Unit,
    onUpgradePrompt: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    devModeEnabled: Boolean,
) {
    // 5. Data Management
    DataManagementSection(
        viewModel = viewModel,
        uiState = uiState,
        password = password,
        onPasswordChange = onPasswordChange,
        onUpgradePrompt = onUpgradePrompt,
    )

    // 6. Help & Support
    HelpSupportSection(onNavigateTo = onNavigateTo)

    // Sandbox / Staging
    DeveloperSandboxSection(devModeEnabled = devModeEnabled, viewModel = viewModel)

    // Version Footer
    VersionFooter(onNavigateTo = onNavigateTo)
}
