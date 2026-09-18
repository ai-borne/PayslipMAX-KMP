package com.payslipmax.pdfparser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.nav.AppNavState
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.OnboardingScreen

/**
 * Renders [MainScaffold] with the onboarding carousel as a translucent-scrim overlay on top of it
 * (not a mutually exclusive branch) — this way a first-time user sees the real Dashboard dimly
 * through the scrim behind the onboarding card, for spatial orientation, rather than a blank gate.
 */
@Composable
internal fun MainContentWithOnboarding(
    navState: AppNavState,
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
    nativeDetailNavigator: ((Screen) -> Unit)?,
    onboardingManager: OnboardingManager,
) {
    var showOnboarding by remember { mutableStateOf(onboardingManager.shouldShowOnboarding()) }

    Box(modifier = Modifier.fillMaxSize()) {
        MainScaffold(
            navState = navState,
            uiState = uiState,
            viewModel = viewModel,
            onPickPdf = onPickPdf,
            onOpenPdf = onOpenPdf,
            onPickBackup = onPickBackup,
            nativeDetailNavigator = nativeDetailNavigator,
            suppressUploadCoachmark = showOnboarding,
        )
        if (showOnboarding) {
            OnboardingScreen(
                onFinished = {
                    showOnboarding = false
                    onboardingManager.onOnboardingCompleted()
                },
                onNavigateToFaq = {
                    showOnboarding = false
                    nativeDetailNavigator?.invoke(Screen.FAQ) ?: navState.push(Screen.FAQ)
                },
            )
        }
    }
}
