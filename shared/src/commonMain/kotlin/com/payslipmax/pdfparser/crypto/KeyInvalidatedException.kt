package com.payslipmax.pdfparser.crypto

/**
 * Exception thrown when the hardware Keystore key or encryption key is invalidated by OS changes.
 */
class KeyInvalidatedException(message: String, cause: Throwable? = null) : Exception(message, cause)
