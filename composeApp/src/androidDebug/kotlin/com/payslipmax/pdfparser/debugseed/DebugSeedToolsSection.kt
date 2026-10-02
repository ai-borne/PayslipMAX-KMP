package com.payslipmax.pdfparser.debugseed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.payslipmax.pdfparser.ui.screens.DeveloperToolsSection
import org.koin.compose.koinInject

/** The synthetic Pay Audit seed, offered in Settings by the debug build only. Its collaborators come from Koin. */
class DebugSeedToolsSection : DeveloperToolsSection {
    @Composable
    override fun Content() {
        val viewModel = koinInject<DebugSeedViewModel>()
        DisposableEffect(viewModel) { onDispose { viewModel.dispose() } }
        DebugSeedSection(viewModel)
    }
}
