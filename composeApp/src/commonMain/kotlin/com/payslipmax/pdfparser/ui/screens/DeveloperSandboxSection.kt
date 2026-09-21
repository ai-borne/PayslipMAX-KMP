package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.subscription.isDebugBuild
import com.payslipmax.pdfparser.subscription.isTestFlightBuild
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.clearAllData
import com.payslipmax.pdfparser.ui.seedMockData
import com.payslipmax.pdfparser.ui.simulateGemmaDownloadFailure
import com.payslipmax.pdfparser.ui.simulateGemmaWaitingForWifi
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Sandbox actions are destructive (clear all data, seed mock data, forced crashes), so the 7-tap
 * unlock alone is not enough: the build must also be debug or TestFlight, matching
 * [DeveloperOverrideSection].
 */
internal fun shouldShowDeveloperSandbox(
    devModeEnabled: Boolean,
    isDebug: Boolean,
    isTestFlight: Boolean,
): Boolean = devModeEnabled && (isDebug || isTestFlight)

@Composable
fun DeveloperSandboxSection(
    devModeEnabled: Boolean,
    viewModel: PayslipViewModel,
) {
    if (shouldShowDeveloperSandbox(devModeEnabled, isDebugBuild(), isTestFlightBuild())) {
        SettingsCategoryHeader(title = AppStrings.settingsStagingTitle)
        SettingsCategoryCard {
            StagingCard(
                onSeedClick = { viewModel.seedMockData() },
                onClearClick = { viewModel.clearAllData() },
                onCrashTestClick = { com.payslipmax.pdfparser.telemetry.triggerTestCrash() },
                onBackgroundCrashTestClick = { com.payslipmax.pdfparser.telemetry.triggerBackgroundTestCrash() },
                onSimulateParserFailureClick = {
                    viewModel.importPayslip(
                        pdfBytes = "UNRECOGNIZED_DOCUMENT_HEADER_SAMPLE_BYTES".encodeToByteArray(),
                        password = "dummy",
                        filename = "unrecognized_military_statement.pdf",
                    )
                },
                onSimulateAiFailureClick = { viewModel.simulateGemmaDownloadFailure() },
                onSimulateAiWaitingForWifiClick = { viewModel.simulateGemmaWaitingForWifi() },
            )
        }
    }
}
