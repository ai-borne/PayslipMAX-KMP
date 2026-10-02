package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable

/**
 * Extension point for developer-only Settings sections that must not exist in the release binary. The
 * code that fills it lives only in the debug source set, so a release build has an empty registry and
 * nothing to render. A section is keyed, so registering the same one again replaces it.
 */
object DeveloperToolsRegistry {
    private val sections = linkedMapOf<String, @Composable () -> Unit>()

    fun register(
        key: String,
        section: @Composable () -> Unit,
    ) {
        sections[key] = section
    }

    fun clear() = sections.clear()

    fun count(): Int = sections.size

    @Composable
    fun Render() {
        sections.values.forEach { it() }
    }
}
