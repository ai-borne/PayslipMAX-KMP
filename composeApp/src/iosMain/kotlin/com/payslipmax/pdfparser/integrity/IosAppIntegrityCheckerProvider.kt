package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.integrity.IosAppIntegrityChecker

actual fun provideAppIntegrityChecker(): AppIntegrityChecker {
    return IosAppIntegrityChecker()
}
