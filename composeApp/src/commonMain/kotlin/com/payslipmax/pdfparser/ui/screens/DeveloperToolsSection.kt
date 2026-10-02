package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.getKoin

/**
 * A developer-only Settings section. Only a debug build binds any (in its variant Koin module), so a
 * release build has none to render and the code behind them is not in the release binary.
 */
interface DeveloperToolsSection {
    @Composable
    fun Content()
}

/** Renders every [DeveloperToolsSection] Koin provides, in binding order; nothing when there are none. */
@Composable
fun DeveloperToolsSections() {
    val koin = getKoin()
    val sections = remember(koin) { koin.getAll<DeveloperToolsSection>() }
    sections.forEach { it.Content() }
}
