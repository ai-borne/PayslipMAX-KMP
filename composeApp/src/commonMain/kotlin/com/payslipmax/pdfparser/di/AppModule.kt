package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.ui.PayslipViewModel
import org.koin.dsl.module

val appModule =
    module {
        factory { PayslipViewModel(get(), get(), get()) }
    }
