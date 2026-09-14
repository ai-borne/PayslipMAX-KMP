package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.clearAllData
import com.payslipmax.pdfparser.ui.seedMockData
import com.payslipmax.pdfparser.ui.simulateGemmaDownloadFailure
import com.payslipmax.pdfparser.ui.simulateGemmaWaitingForWifi
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun DeveloperSandboxSection(
    devModeEnabled: Boolean,
    viewModel: PayslipViewModel,
) {
    if (devModeEnabled) {
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
