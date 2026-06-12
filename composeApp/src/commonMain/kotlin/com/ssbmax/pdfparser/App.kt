package com.ssbmax.pdfparser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.screens.DashboardScreen
import com.ssbmax.pdfparser.ui.screens.ImportScreen
import com.ssbmax.pdfparser.ui.screens.InsightsScreen
import com.ssbmax.pdfparser.ui.screens.PayslipReplicaScreen
import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.PDFParserTheme

enum class Screen {
    Dashboard,
    Explorer,
    Insights,
    Import,
}

@Composable
fun App(
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    PDFParserTheme {
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
                    Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    Screen.Explorer -> PayslipReplicaScreen(viewModel = viewModel)
                    Screen.Insights -> InsightsScreen(viewModel = viewModel)
                    Screen.Import -> {
                        ImportScreen(
                            viewModel = viewModel,
                            onPickPdfTrigger = { password ->
                                onPickPdf { bytes, name ->
                                    viewModel.importPayslip(bytes, password, name)
                                }
                            },
                        )
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
            selected = currentScreen == Screen.Explorer,
            onClick = { onNavigate(Screen.Explorer) },
            label = { Text(AppStrings.navigationExplorer) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Insights,
            onClick = { onNavigate(Screen.Insights) },
            label = { Text(AppStrings.navigationInsights) },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Import,
            onClick = { onNavigate(Screen.Import) },
            label = { Text(AppStrings.navigationImport) },
            icon = { Icon(Icons.Default.Share, contentDescription = null) },
        )
    }
}
