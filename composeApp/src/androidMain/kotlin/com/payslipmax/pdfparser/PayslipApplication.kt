package com.payslipmax.pdfparser

import android.app.Application
import com.payslipmax.pdfparser.di.appModule
import com.payslipmax.pdfparser.di.sharedModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class PayslipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.payslipmax.pdfparser.crypto.ContextHolder.context = this
        // Idempotent: Koin's GlobalContext is a JVM-wide singleton. Real devices only ever call
        // onCreate() once per process, so this guard is a no-op there — it only matters under
        // Robolectric, where multiple test classes share a JVM fork and each reconstructs this
        // Application, which would otherwise throw KoinApplicationAlreadyStartedException whenever
        // a prior test's stopKoin() teardown hadn't run yet in that fork.
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(sharedModule, appModule)
            }
        }
    }
}
