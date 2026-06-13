package com.ssbmax.pdfparser.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ssbmax.pdfparser.database.PayslipDatabase
import com.ssbmax.pdfparser.database.getDatabaseBuilder
import com.ssbmax.pdfparser.parser.PdfParser
import com.ssbmax.pdfparser.repository.PayslipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.startKoin
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
            com.ssbmax.pdfparser.parser.PlatformPdfParser()
        }

        single {
            PayslipRepository(get(), get())
        }

        single<com.ssbmax.pdfparser.backup.BackupManager> {
            com.ssbmax.pdfparser.backup.PlatformBackupManager(get())
        }

        single {
            com.ssbmax.pdfparser.insights.GeminiService()
        }
    }

/**
 * Standard Koin initialization helper for iOS target.
 */
fun initKoin() =
    startKoin {
        modules(sharedModule)
    }
