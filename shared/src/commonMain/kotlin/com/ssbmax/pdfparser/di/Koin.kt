package com.ssbmax.pdfparser.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ssbmax.pdfparser.database.PayslipDatabase
import com.ssbmax.pdfparser.database.getDatabaseBuilder
import com.ssbmax.pdfparser.parser.PdfParser
import com.ssbmax.pdfparser.repository.PayslipRepository
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
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
            io.ktor.client.HttpClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(
                        kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                            isLenient = true
                        },
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
            }
        }

        single {
            com.ssbmax.pdfparser.insights.GeminiProxyService(get())
        }

        single {
            com.ssbmax.pdfparser.repository.FinancialIntelligenceRepository(get(), get())
        }

        single {
            com.ssbmax.pdfparser.repository.CloudSyncRepository(get(), get())
        }
    }

/**
 * Standard Koin initialization helper for iOS target.
 */
fun initKoin() =
    startKoin {
        modules(sharedModule)
    }
