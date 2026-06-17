package com.ssbmax.pdfparser

import android.app.Application
import com.ssbmax.pdfparser.di.appModule
import com.ssbmax.pdfparser.di.sharedModule
import org.koin.core.context.startKoin

class PayslipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.ssbmax.pdfparser.crypto.ContextHolder.context = this
        startKoin {
            modules(sharedModule, appModule)
        }
    }
}
