package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.insights.gemma.GemmaBaseModelInstaller
import com.payslipmax.pdfparser.insights.gemma.provideGemmaBaseModelInstaller
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.PayAuditViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule =
    module {
        // One installer for every PayslipViewModel: the iOS installer publishes its progress
        // through process-wide statics, so a second instance would orphan the first's subscriber.
        single<GemmaBaseModelInstaller> { provideGemmaBaseModelInstaller() }
        // One manager so App, Dashboard and Pay Audit read and write the same first-run flags.
        single { OnboardingManager() }
        factory { PayslipViewModel(get(), get(), get()) }
        factory { PayAuditViewModel() }
    }

/**
 * The single module list both platforms start Koin with (Android's `PayslipApplication`, iOS's
 * `ensureKoin`), so a binding added once exists on both. [platformModules] load last, so a platform or
 * build variant can add bindings or replace a default.
 */
fun appKoinModules(platformModules: List<Module> = emptyList()): List<Module> = listOf(sharedModule, appModule) + platformModules
