package com.payslipmax.pdfparser.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.repository.PayslipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule: Module =
    module {
        single<PayslipDatabase> {
            getDatabaseBuilder()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }

        single {
            get<PayslipDatabase>().payslipDao()
        }

        single<PdfParser> {
            com.payslipmax.pdfparser.parser.PlatformPdfParser()
        }

        single {
            PayslipRepository(get(), get())
        }

        single {
            com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository(get())
        }

        single<com.payslipmax.pdfparser.telemetry.CrashReporter> {
            com.payslipmax.pdfparser.telemetry.provideCrashReporter()
        }

        single {
            com.payslipmax.pdfparser.telemetry.InstallationIdManager()
        }
    }
