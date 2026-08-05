package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.integrity.AndroidAppIntegrityChecker
import com.payslipmax.pdfparser.shared.BuildConfig

actual fun provideAppIntegrityChecker(): AppIntegrityChecker {
    val context =
        ContextHolder.context ?: return object : AppIntegrityChecker {
            override suspend fun checkIntegrity(): AppIntegrityStatus = AppIntegrityStatus.Valid
        }
    return AndroidAppIntegrityChecker(
        context = context,
        isDebug = BuildConfig.DEBUG,
    )
}
