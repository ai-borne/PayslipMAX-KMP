package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyInvalidationResilienceTest {
    @Test
    fun testKeyInvalidatedException_typedErrorMessage() {
        val exception = KeyInvalidatedException("Keystore key invalidated due to OS update")
        assertEquals("Keystore key invalidated due to OS update", exception.message)
    }

    @Test
    fun testDecryptWithTamperedData_returnsFailure() {
        val password = "TestPassword123!"
        val invalidBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28)
        val result = CryptoHelper.decrypt(invalidBytes, password)
        assertTrue(result.isFailure)
    }
}
