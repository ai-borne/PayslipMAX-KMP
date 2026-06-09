package com.ssbmax.pdfparser.di

import com.ssbmax.pdfparser.ui.PayslipViewModel
import org.koin.dsl.module

val appModule = module {
    factory { PayslipViewModel(get(), get()) }
}
