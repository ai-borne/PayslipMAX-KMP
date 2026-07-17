package com.payslipmax.pdfparser

import android.app.Application
import com.payslipmax.pdfparser.di.appModule
import com.payslipmax.pdfparser.di.sharedModule
import org.koin.core.context.startKoin

class PayslipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.payslipmax.pdfparser.crypto.ContextHolder.context = this
        startKoin {
            modules(sharedModule, appModule)
        }
    }
}
