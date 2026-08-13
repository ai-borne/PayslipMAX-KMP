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
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit = {},
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
        onPickBackup = onPickBackup,
        modifier = modifier,
    )

    if (showUpgradeSheet) {
        val premiumPrice by viewModel.premiumPriceState.collectAsState()
        PremiumUpgradeBottomSheet(
            onDismissRequest = { showUpgradeSheet = false },
            onUnlockClick = { viewModel.launchPurchaseFlow() },
            price = premiumPrice,
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
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState(uiState.settingsScrollValue)
    DisposableEffect(Unit) {
        onDispose { viewModel.saveSettingsScrollPosition(scrollState.value) }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(AppDimensions.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        SettingsHeader(onTitleClick = onHeaderClick)
        PrimarySettingsGroup(viewModel, uiState, onUpgradePrompt, onNavigateTo)
        SecondarySettingsGroup(
            viewModel = viewModel,
            uiState = uiState,
            password = password,
            onPasswordChange = onPasswordChange,
            onUpgradePrompt = onUpgradePrompt,
            onNavigateTo = onNavigateTo,
            devModeEnabled = devModeEnabled,
            onPickBackup = onPickBackup,
        )
    }
}

@Composable
private fun PrimarySettingsGroup(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    onUpgradePrompt: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    // 1. Account & Subscription
    SettingsCategoryHeader(title = AppStrings.settingsAccountSubscriptionHeader)

    // Profile Section
    ProfileSection(viewModel = viewModel, uiState = uiState)

    // PayslipMax Premium Section
    PremiumSection(viewModel = viewModel, uiState = uiState, onUpgradePrompt = onUpgradePrompt, onNavigateTo = onNavigateTo)

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
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
) {
    // 5. Data Management
    DataManagementSection(
        viewModel = viewModel,
        uiState = uiState,
        password = password,
        onPasswordChange = onPasswordChange,
        onUpgradePrompt = onUpgradePrompt,
        onPickBackup = onPickBackup,
    )

    // 6. Help & Support
    HelpSupportSection(onNavigateTo = onNavigateTo)

    // Developer entitlement override (debug builds only; no-op in release)
    DeveloperOverrideSection(viewModel = viewModel)

    // Sandbox / Staging
    DeveloperSandboxSection(devModeEnabled = devModeEnabled, viewModel = viewModel)

    // Version Footer
    VersionFooter()
}
