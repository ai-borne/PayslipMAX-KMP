package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.debugseed.DebugSeedToolsSection
import com.payslipmax.pdfparser.ui.screens.DeveloperToolsSection
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** The debug build is the only one that carries developer tooling, and it is wired through Koin, not a global. */
class DebugVariantModulesTest {
    @Test
    fun theDebugVariantProvidesExactlyTheSeedSection() {
        val koin = koinApplication { modules(appKoinModules(variantModules)) }.koin

        val sections = koin.getAll<DeveloperToolsSection>()

        assertEquals(1, sections.size, "debug must offer exactly one developer section: $sections")
        assertIs<DebugSeedToolsSection>(sections.single())
    }

    @Test
    fun theSeedSectionIsNotProvidedWithoutTheVariantModules() {
        val koin = koinApplication { modules(appKoinModules()) }.koin

        assertEquals(emptyList(), koin.getAll<DeveloperToolsSection>(), "shared modules must never bind a developer section")
    }
}
