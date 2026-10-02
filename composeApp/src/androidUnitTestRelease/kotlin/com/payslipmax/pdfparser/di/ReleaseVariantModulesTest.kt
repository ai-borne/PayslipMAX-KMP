package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.ui.screens.DeveloperToolsSection
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/** A release build must carry no developer tooling: nothing in its Koin graph can render a developer section. */
class ReleaseVariantModulesTest {
    @Test
    fun theReleaseVariantAddsNoModules() {
        assertEquals(emptyList(), variantModules)
    }

    @Test
    fun theReleaseGraphProvidesNoDeveloperSection() {
        val koin = koinApplication { modules(appKoinModules(variantModules)) }.koin

        assertEquals(emptyList(), koin.getAll<DeveloperToolsSection>())
    }
}
