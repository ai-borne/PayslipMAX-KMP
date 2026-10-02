package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.debugseed.DebugSeedToolsSection
import com.payslipmax.pdfparser.debugseed.DebugSeedViewModel
import com.payslipmax.pdfparser.debugseed.DebugSeeder
import com.payslipmax.pdfparser.ui.screens.DeveloperToolsSection
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/** The synthetic Pay Audit seed: a Settings section and the collaborators behind it, all resolved by Koin. */
private val debugSeedModule =
    module {
        factory { DebugSeeder(get(), get()) }
        factory { DebugSeedViewModel(get()) }
        factory { DebugSeedToolsSection() } bind DeveloperToolsSection::class
    }

/** Extra Koin modules of the debug build. The release source set declares the same name with none. */
val variantModules: List<Module> = listOf(debugSeedModule)
