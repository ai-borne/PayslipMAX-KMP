package com.payslipmax.pdfparser.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * @param showGuide adds the Claim Guide tab before Settings; false keeps the four tabs shipped before it.
 * @param onGuideReselected re-tapping the active Guide tab returns to Guide Home (owner decision 2026-10-07).
 */
@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    showGuide: Boolean = false,
    onGuideReselected: () -> Unit = {},
) {
    val itemColors = appNavItemColors()
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = currentScreen == Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) },
            label = { Text(AppStrings.navigationHome) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = currentScreen == Screen.History,
            onClick = { onNavigate(Screen.History) },
            label = { Text(AppStrings.navigationHistory) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Insights,
            onClick = { onNavigate(Screen.Insights) },
            label = { Text(AppStrings.navigationInsights) },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            colors = itemColors,
        )
        if (showGuide) {
            NavigationBarItem(
                selected = currentScreen == Screen.Guide,
                onClick = { if (currentScreen == Screen.Guide) onGuideReselected() else onNavigate(Screen.Guide) },
                label = { Text(GuideStrings.tabLabel) },
                icon = { Icon(Icons.Default.Place, contentDescription = null) },
                colors = itemColors,
            )
        }
        NavigationBarItem(
            selected = currentScreen == Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            label = { Text(AppStrings.navigationSettings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            colors = itemColors,
        )
    }
}

/** The selected tab wears the brand blue, like every other selected or active control in the app. */
@Composable
private fun appNavItemColors(): NavigationBarItemColors =
    NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    )
