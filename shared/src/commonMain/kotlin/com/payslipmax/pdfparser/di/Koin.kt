package com.payslipmax.pdfparser.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.repository.PayslipBackupService
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

        // The Part II tables always exist (schema 15), so the DAO and the backup of its rows are bound whatever the flag says;
        // only the repository the screens use is loaded with the feature (see part2Module).
        single {
            get<PayslipDatabase>().part2Dao()
        }

        single {
            Part2Backup(get())
        }

        single {
            PayslipBackupService(get(), payslipRepository = get(), intelligence = get(), part2Backup = get())
        }

        single<com.payslipmax.pdfparser.telemetry.CrashReporter> {
            com.payslipmax.pdfparser.telemetry.provideCrashReporter()
        }

        single {
            com.payslipmax.pdfparser.telemetry.InstallationIdManager()
        }
    }
