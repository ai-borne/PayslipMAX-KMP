package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaValidatorTest {
    @Test
    fun validTotalsPassValidation() {
        val result =
            SchemaValidator.validate(
                grossPay = 100000.0,
                totalDeductions = 20000.0,
                netRemittance = 80000.0,
                creditsSum = 100000.0,
                debitsSum = 20000.0,
            )

        assertTrue(result.isValid)
        assertEquals(0.0, result.grossMismatch)
        assertEquals(0.0, result.deductionsMismatch)
        assertEquals(0.0, result.netResidual)
    }

    @Test
    fun mismatchedTotalsFailValidation() {
        val result =
            SchemaValidator.validate(
                grossPay = 100000.0,
                totalDeductions = 20000.0,
                netRemittance = 75000.0,
                creditsSum = 90000.0,
                debitsSum = 20000.0,
            )

        assertFalse(result.isValid)
        assertEquals(10000.0, result.grossMismatch)
        assertEquals(5000.0, result.netResidual)
    }
}
