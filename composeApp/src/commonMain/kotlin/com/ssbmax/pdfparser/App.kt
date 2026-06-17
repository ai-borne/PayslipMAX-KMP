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
import com.ssbmax.pdfparser.ui.PayslipViewModel
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
                    when (currentScreen) {
                        Screen.Dashboard -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onPickPdfTrigger = { password ->
                                    onPickPdf { bytes, name ->
                                        viewModel.importPayslip(bytes, password, name)
                                    }
                                },
                            )
                        }
                        Screen.History -> HistoryScreen(viewModel = viewModel, onOpenPdf = onOpenPdf)
                        Screen.Insights -> InsightsScreen(viewModel = viewModel)
                        Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
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
