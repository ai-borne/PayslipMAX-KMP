package com.payslipmax.pdfparser.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppIntegrityCheckerTest {
    @Test
    fun validStatus_allowsAppToRun() {
        val status: AppIntegrityStatus = AppIntegrityStatus.Valid
        assertTrue(status.isAllowedToRun)
    }

    @Test
    fun unknownStatus_allowsAppToRunAsFallback() {
        val status: AppIntegrityStatus = AppIntegrityStatus.Unknown
        assertTrue(status.isAllowedToRun)
    }

    @Test
    fun sideloadedStatus_disallowsAppToRun() {
        val status: AppIntegrityStatus = AppIntegrityStatus.Sideloaded("Untrusted installer")
        assertFalse(status.isAllowedToRun)
        assertEquals("Untrusted installer", (status as AppIntegrityStatus.Sideloaded).reason)
    }

    @Test
    fun tamperedStatus_disallowsAppToRun() {
        val status: AppIntegrityStatus = AppIntegrityStatus.Tampered("Signature mismatch")
        assertFalse(status.isAllowedToRun)
        assertEquals("Signature mismatch", (status as AppIntegrityStatus.Tampered).reason)
    }

    @Test
    fun fakeIntegrityChecker_returnsConfiguredStatusAndTracksInvocations() =
        runTest {
            val fake = FakeAppIntegrityChecker(AppIntegrityStatus.Sideloaded("APK sideloaded"))
            val result = fake.checkIntegrity()

            assertEquals(1, fake.checkCount)
            assertTrue(result is AppIntegrityStatus.Sideloaded)
            assertEquals("APK sideloaded", (result as AppIntegrityStatus.Sideloaded).reason)
        }
}
