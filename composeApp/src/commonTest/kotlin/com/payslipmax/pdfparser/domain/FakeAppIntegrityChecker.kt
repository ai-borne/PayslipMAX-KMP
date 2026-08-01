package com.payslipmax.pdfparser.domain

/**
 * Test double implementation of [AppIntegrityChecker] for unit testing.
 */
class FakeAppIntegrityChecker(
    var statusToReturn: AppIntegrityStatus = AppIntegrityStatus.Valid,
) : AppIntegrityChecker {
    var checkCount: Int = 0
        private set

    override suspend fun checkIntegrity(): AppIntegrityStatus {
        checkCount++
        return statusToReturn
    }
}
