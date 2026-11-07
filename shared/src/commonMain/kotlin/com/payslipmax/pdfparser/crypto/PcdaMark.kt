package com.payslipmax.pdfparser.crypto

/**
 * The four bytes "PCDA" that start every plaintext this app encrypts. A decrypt that authenticates but does not start with them was sealed by
 * something else, so it is refused like a wrong password. One copy here, used by both platforms' `encrypt` and `decrypt`.
 */
internal object PcdaMark {
    val BYTES: ByteArray = "PCDA".encodeToByteArray()

    /** [plain] without its mark, or a [DecryptionFailedException] when it is shorter than the mark or starts with anything else. */
    fun open(plain: ByteArray): Result<ByteArray> {
        if (plain.size < BYTES.size) return Result.failure(DecryptionFailedException())
        for (i in BYTES.indices) if (plain[i] != BYTES[i]) return Result.failure(DecryptionFailedException())
        return Result.success(plain.copyOfRange(BYTES.size, plain.size))
    }
}
