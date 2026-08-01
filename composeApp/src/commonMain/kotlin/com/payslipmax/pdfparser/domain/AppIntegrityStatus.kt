package com.payslipmax.pdfparser.domain

/**
 * Single Source of Truth (SSOT) domain model representing the integrity status
 * of the application instance.
 */
sealed class AppIntegrityStatus {
    data object Valid : AppIntegrityStatus()

    data class Sideloaded(val reason: String) : AppIntegrityStatus()

    data class Tampered(val reason: String) : AppIntegrityStatus()

    data object Unknown : AppIntegrityStatus()

    val isAllowedToRun: Boolean
        get() = this is Valid || this is Unknown
}
