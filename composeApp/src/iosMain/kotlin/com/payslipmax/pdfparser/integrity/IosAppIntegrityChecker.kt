package com.payslipmax.pdfparser.integrity

import com.payslipmax.pdfparser.domain.AppIntegrityChecker
import com.payslipmax.pdfparser.domain.AppIntegrityStatus

/**
 * iOS implementation of [AppIntegrityChecker].
 * iOS platform sandbox & App Store code signing natively restrict unauthorized sideloading.
 */
class IosAppIntegrityChecker : AppIntegrityChecker {
    override suspend fun checkIntegrity(): AppIntegrityStatus {
        return AppIntegrityStatus.Valid
    }
}
