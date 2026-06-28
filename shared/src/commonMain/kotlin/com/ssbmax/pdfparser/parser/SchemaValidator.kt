package com.ssbmax.pdfparser.parser

import kotlin.math.abs

/** Validation result for Tier 7 Schema Validator. */
data class SchemaValidationResult(
    val isValid: Boolean,
    val grossMismatch: Double,
    val deductionsMismatch: Double,
    val netResidual: Double,
)

/**
 * Tier 7 — Schema Validator.
 * Acts as the final gatekeeper in the 7-tier parsing pipeline, validating mathematical
 * invariants (Gross Pay sum, Deductions sum, and Net Remittance balance) after extraction and confidence scoring.
 */
object SchemaValidator {
    private const val TOLERANCE = 2.0

    fun validate(
        grossPay: Double,
        totalDeductions: Double,
        netRemittance: Double,
        creditsSum: Double,
        debitsSum: Double,
    ): SchemaValidationResult {
        val grossMismatch = abs(grossPay - creditsSum)
        val deductionsMismatch = abs(totalDeductions - debitsSum)
        val expectedNet = grossPay - totalDeductions
        val netResidual = abs(expectedNet - netRemittance)

        val isValid = grossMismatch <= TOLERANCE && deductionsMismatch <= TOLERANCE && netResidual <= TOLERANCE
        return SchemaValidationResult(
            isValid = isValid,
            grossMismatch = grossMismatch,
            deductionsMismatch = deductionsMismatch,
            netResidual = netResidual,
        )
    }
}
