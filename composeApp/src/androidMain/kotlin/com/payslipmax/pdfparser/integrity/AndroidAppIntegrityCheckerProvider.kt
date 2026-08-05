package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.integrity.AndroidAppIntegrityChecker
import com.payslipmax.pdfparser.shared.BuildConfig

actual fun provideAppIntegrityChecker(): AppIntegrityChecker {
    val context =
        ContextHolder.context ?: return object : AppIntegrityChecker {
            override suspend fun checkIntegrity(): AppIntegrityStatus = AppIntegrityStatus.Valid
        }
    val isTestEnvironment = android.os.Build.FINGERPRINT == "robolectric" || android.os.Build.HARDWARE == "robolectric"
    return AndroidAppIntegrityChecker(
        context = context,
        isDebug = BuildConfig.DEBUG || isTestEnvironment,
    )
}
