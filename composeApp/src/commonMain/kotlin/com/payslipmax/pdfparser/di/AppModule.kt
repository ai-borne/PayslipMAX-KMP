package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.insights.gemma.GemmaBaseModelInstaller
import com.payslipmax.pdfparser.insights.gemma.provideGemmaBaseModelInstaller
import com.payslipmax.pdfparser.ui.PayslipViewModel
import org.koin.dsl.module

val appModule =
    module {
        // One installer for every PayslipViewModel: the iOS installer publishes its progress
        // through process-wide statics, so a second instance would orphan the first's subscriber.
        single<GemmaBaseModelInstaller> { provideGemmaBaseModelInstaller() }
        factory { PayslipViewModel(get(), get(), get()) }
    }
