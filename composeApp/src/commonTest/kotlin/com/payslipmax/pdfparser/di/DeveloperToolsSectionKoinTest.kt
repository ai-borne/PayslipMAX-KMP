package com.payslipmax.pdfparser.di

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.screens.DeveloperToolsSection
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class AlphaSection : DeveloperToolsSection {
    @Composable
    override fun Content() = Unit
}

private class BetaSection : DeveloperToolsSection {
    @Composable
    override fun Content() = Unit
}

/**
 * Developer sections are discovered by Koin multibinding rather than a mutable global, and Settings relies on
 * `getAll`. This runs on the JVM and on Kotlin/Native, so a platform that resolved it differently is caught.
 */
class DeveloperToolsSectionKoinTest {
    @Test
    fun everyBoundSectionIsReturnedAndNothingElse() {
        val koin =
            koinApplication {
                modules(module { factory { AlphaSection() } bind DeveloperToolsSection::class }, module { factory { BetaSection() } bind DeveloperToolsSection::class })
            }.koin

        val types = koin.getAll<DeveloperToolsSection>().map { it::class }.toSet()

        assertEquals(setOf(AlphaSection::class, BetaSection::class), types)
    }

    @Test
    fun withNoBindingThereAreNoSections() {
        assertTrue(koinApplication { modules(appKoinModules()) }.koin.getAll<DeveloperToolsSection>().isEmpty())
    }
}
