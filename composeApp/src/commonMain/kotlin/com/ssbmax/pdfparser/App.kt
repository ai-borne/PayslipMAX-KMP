package com.ssbmax.pdfparser

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ssbmax.pdfparser.ui.*
import com.ssbmax.pdfparser.ui.screens.DashboardScreen
import com.ssbmax.pdfparser.ui.screens.HistoryScreen
import com.ssbmax.pdfparser.ui.screens.InsightsScreen
import com.ssbmax.pdfparser.ui.screens.LockScreen
import com.ssbmax.pdfparser.ui.screens.SettingsScreen
import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.PDFParserTheme

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

@Composable
fun App(
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    val uiState by viewModel.uiState.collectAsState()

    val darkTheme =
        when (uiState.appTheme) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }

    PDFParserTheme(darkTheme = darkTheme) {
        if (uiState.isLockEnabled && uiState.isAppLocked) {
            LockScreen(
                onUnlock = { pin -> viewModel.verifyPin(pin) },
                onPickPdf = onPickPdf,
                onResetPin = { bytes, pwd, name, onResult ->
                    viewModel.resetPinWithPdf(bytes, pwd, name, onResult)
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    BottomBar(
                        currentScreen = currentScreen,
                        onNavigate = { currentScreen = it },
                    )
                },
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    ScreenContent(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        onPickPdf = onPickPdf,
                        onOpenPdf = onOpenPdf,
                        onNavigate = { currentScreen = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    currentScreen: Screen,
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    when (currentScreen) {
        Screen.Dashboard -> DashboardScreen(
            viewModel = viewModel,
            onPickPdfTrigger = { password -> onPickPdf { bytes, name -> viewModel.importPayslip(bytes, password, name) } }
        )
        Screen.History -> HistoryScreen(
            viewModel = viewModel,
            onOpenPdf = onOpenPdf,
            onNavigateToInsights = { onNavigate(Screen.Insights) }
        )
        Screen.Insights -> InsightsScreen(viewModel = viewModel, onNavigateTo = onNavigate)
        Screen.Settings -> SettingsScreen(viewModel = viewModel, onNavigateTo = onNavigate)
        Screen.Representation -> com.ssbmax.pdfparser.ui.screens.RepresentationScreen(
            viewModel = viewModel, onBack = { onNavigate(Screen.Insights) }
        )
        Screen.TaxPlanning -> com.ssbmax.pdfparser.ui.screens.TaxPlanningScreen(
            viewModel = viewModel, onBack = { onNavigate(Screen.Insights) }
        )
        Screen.RetirementPlanning -> com.ssbmax.pdfparser.ui.screens.RetirementPlanningScreen(
            viewModel = viewModel, onBack = { onNavigate(Screen.Insights) }
        )
        Screen.HelpLegal -> com.ssbmax.pdfparser.ui.screens.HelpLegalScreen(
            onBack = { onNavigate(Screen.Settings) }
        )
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
            selected =
                currentScreen == Screen.Insights ||
                    currentScreen == Screen.Representation ||
                    currentScreen == Screen.TaxPlanning ||
                    currentScreen == Screen.RetirementPlanning,
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
