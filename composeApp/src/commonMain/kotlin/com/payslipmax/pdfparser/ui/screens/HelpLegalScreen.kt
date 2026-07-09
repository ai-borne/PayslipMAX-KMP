package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.LegalStrings

@Composable
fun HelpLegalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screen: Screen = Screen.HelpLegal,
) {
    val title =
        when (screen) {
            Screen.FAQ -> AppStrings.settingsHelpFaqTitle
            Screen.PrivacyPolicy -> AppStrings.settingsHelpPrivacyTitle
            else -> AppStrings.settingsHelpDocsHeader
        }
    Scaffold(
        topBar = { HelpLegalTopAppBar(title = title, onBack = onBack) },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
        ) {
            when (screen) {
                Screen.FAQ -> FaqContent()
                Screen.PrivacyPolicy -> PrivacyContent()
                else -> AllLegalContent()
            }
        }
    }
}

@Composable
private fun FaqContent() {
    DocSection(
        title = AppStrings.settingsHelpFaqTitle,
        content = LegalStrings.settingsHelpFaqContent,
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    DocSection(
        title = AppStrings.settingsHelpDisclaimerTitle,
        content = LegalStrings.settingsHelpDisclaimerContent,
    )
}

@Composable
private fun PrivacyContent() {
    DocSection(
        title = AppStrings.settingsHelpPrivacyTitle,
        content = LegalStrings.settingsHelpPrivacyContent,
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    DocSection(
        title = AppStrings.settingsHelpAiTitle,
        content = LegalStrings.settingsHelpAiContent,
    )
}

@Composable
private fun AllLegalContent() {
    FaqContent()
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    PrivacyContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpLegalTopAppBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = AppStrings.btnBack,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
private fun DocSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
