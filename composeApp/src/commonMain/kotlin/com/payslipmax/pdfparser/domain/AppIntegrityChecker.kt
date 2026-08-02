package com.payslipmax.pdfparser.domain

/**
 * Domain interface contract for validating application integrity and verifying
 * that the app was installed from an authorized source (e.g. Google Play Store or App Store).
 */
interface AppIntegrityChecker {
    suspend fun checkIntegrity(): AppIntegrityStatus
}
