package com.payslipmax.pdfparser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.payslipmax.pdfparser.nav.AppNavState
import com.payslipmax.pdfparser.ui.*
import com.payslipmax.pdfparser.ui.screens.BaseModelDownloadBanner
import com.payslipmax.pdfparser.ui.screens.DashboardScreen
import com.payslipmax.pdfparser.ui.screens.HistoryScreen
import com.payslipmax.pdfparser.ui.screens.InsightsScreen
import com.payslipmax.pdfparser.ui.screens.LockScreen
import com.payslipmax.pdfparser.ui.screens.SettingsScreen
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.PDFParserTheme
import com.payslipmax.pdfparser.ui.theme.resolveDarkTheme

enum class Screen {
    Dashboard,
    History,
    Insights,
    Settings,
    Representation,
    TaxPlanning,
    RetirementPlanning,
    HelpLegal,
}

/** The four bottom-tab roots; the remaining [Screen] values are pushed detail screens. */
internal val Screen.isTabRoot: Boolean
    get() = this == Screen.Dashboard || this == Screen.History || this == Screen.Insights || this == Screen.Settings

/**
 * Persists [AppNavState] across process death via two primitive [Screen] name slots
 * (decision 9). Restoration is crash-guarded: if a saved constant was renamed or removed
 * in a later build, [restoreScreen] falls back rather than throwing on cold start.
 */
internal val AppNavStateSaver: Saver<AppNavState, Any> =
    listSaver(
        // listSaver requires a non-null element type, so absent detail is stored as an empty slot.
        save = { listOf(it.currentTab.name, it.activeDetail?.name ?: "") },
        restore = {
            AppNavState(
                currentTab = restoreScreen(it.getOrNull(0))?.takeIf(Screen::isTabRoot) ?: Screen.Dashboard,
                activeDetail = restoreScreen(it.getOrNull(1))?.takeIf { s -> !s.isTabRoot },
            )
        },
    )

private fun restoreScreen(name: String?): Screen? =
    name?.let { runCatching { Screen.valueOf(it) }.getOrNull() }

/**
 * @param navState hoisted so iOS can share one instance between the root Compose tree and its
 *   [com.payslipmax.pdfparser.nav.NavBridge]; Android uses the default `rememberSaveable` instance.
 * @param nativeDetailNavigator when non-null (iOS), a detail navigation request is routed to the
 *   native `UINavigationController` instead of being rendered inline, and the root tree keeps
 *   showing tab content + bottom bar (the pushed native VC covers it). Null on Android, where
 *   details render inline via [AppNavState.push].
 */
@Composable
fun App(
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    navState: AppNavState = rememberSaveable(saver = AppNavStateSaver) { AppNavState() },
    nativeDetailNavigator: ((Screen) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    PDFParserTheme(darkTheme = resolveDarkTheme(uiState.appTheme)) {
        if (uiState.isLockEnabled && uiState.isAppLocked) {
            // No BackHandler is composed here, so system back falls through to the OS default
            // (backgrounding the app) — locked content can never be revealed via back (decision 5).
            LockScreen(
                onUnlock = { pin -> viewModel.verifyPin(pin) },
                onPickPdf = onPickPdf,
                onResetPin = { bytes, pwd, name, onResult ->
                    viewModel.resetPinWithPdf(bytes, pwd, name, onResult)
                },
            )
        } else {
            MainScaffold(
                navState = navState,
                uiState = uiState,
                viewModel = viewModel,
                onPickPdf = onPickPdf,
                onOpenPdf = onOpenPdf,
                nativeDetailNavigator = nativeDetailNavigator,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MainScaffold(
    navState: AppNavState,
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    nativeDetailNavigator: ((Screen) -> Unit)?,
) {
    // On iOS details are hosted as native VCs covering this tree, so the root scaffold keeps
    // showing tab content + bottom bar and never handles back itself (native nav owns it).
    val hostDetailsNatively = nativeDetailNavigator != null
    Scaffold(
        bottomBar = {
            // Detail screens are pushed on top and hide the tab bar (decision 8) — but on iOS the
            // native VC covers the whole root tree, so the bar stays here (hidden behind it).
            if (hostDetailsNatively || navState.activeDetail == null) {
                BottomBar(
                    currentScreen = navState.currentTab,
                    onNavigate = { navState.switchTab(it) },
                )
            }
        },
    ) { paddingValues ->
        // System back pops a pushed detail; at a tab root it stays disabled so back exits.
        // Never enabled on iOS — the native UINavigationController handles back there.
        BackHandler(enabled = !hostDetailsNatively && navState.activeDetail != null) { navState.pop() }
        Column(modifier = Modifier.padding(paddingValues)) {
            BaseModelDownloadBanner(uiState = uiState)
            Box(modifier = Modifier.weight(1f)) {
                ScreenContent(
                    navState = navState,
                    viewModel = viewModel,
                    onPickPdf = onPickPdf,
                    onOpenPdf = onOpenPdf,
                    nativeDetailNavigator = nativeDetailNavigator,
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    navState: AppNavState,
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    nativeDetailNavigator: ((Screen) -> Unit)?,
) {
    val activeDetail = navState.activeDetail
    if (activeDetail != null && nativeDetailNavigator == null) {
        DetailContent(detail = activeDetail, viewModel = viewModel, onBack = { navState.pop() })
    } else {
        // Route by destination: tab roots switch tabs; detail screens push natively on iOS
        // (nativeDetailNavigator) or render inline on Android (SSOT via isTabRoot).
        val onNavigate: (Screen) -> Unit = {
            if (it.isTabRoot) navState.switchTab(it) else nativeDetailNavigator?.invoke(it) ?: navState.push(it)
        }
        when (navState.currentTab) {
            Screen.History ->
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenPdf = onOpenPdf,
                    onNavigateToInsights = { onNavigate(Screen.Insights) },
                )
            Screen.Insights -> InsightsScreen(viewModel = viewModel, onNavigateTo = onNavigate)
            Screen.Settings -> SettingsScreen(viewModel = viewModel, onNavigateTo = onNavigate)
            else ->
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = { password -> onPickPdf { bytes, name -> viewModel.importPayslip(bytes, password, name) } },
                )
        }
    }
}

@Composable
private fun DetailContent(
    detail: Screen,
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
) {
    when (detail) {
        Screen.Representation ->
            com.payslipmax.pdfparser.ui.screens.RepresentationScreen(viewModel = viewModel, onBack = onBack)
        Screen.TaxPlanning ->
            com.payslipmax.pdfparser.ui.screens.TaxPlanningScreen(viewModel = viewModel, onBack = onBack)
        Screen.RetirementPlanning ->
            com.payslipmax.pdfparser.ui.screens.RetirementPlanningScreen(viewModel = viewModel, onBack = onBack)
        else ->
            com.payslipmax.pdfparser.ui.screens.HelpLegalScreen(onBack = onBack)
    }
}

@Composable
private fun BottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = currentScreen == Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) },
            label = { Text(AppStrings.navigationHome) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
        )
        NavigationBarItem(
            selected = currentScreen == Screen.History,
            onClick = { onNavigate(Screen.History) },
            label = { Text(AppStrings.navigationHistory) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Insights,
            onClick = { onNavigate(Screen.Insights) },
            label = { Text(AppStrings.navigationInsights) },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            label = { Text(AppStrings.navigationSettings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
        )
    }
}
