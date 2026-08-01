package com.payslipmax.pdfparser.integrity

import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.domain.AppIntegrityStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidAppIntegrityCheckerTest {
    @Test
    fun checkIntegrity_returnsValid_whenInstallerIsPlayStore() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val checker =
                AndroidAppIntegrityChecker(
                    context = context,
                    isDebug = false,
                    installerNameProvider = { "com.android.vending" },
                )
            val status = checker.checkIntegrity()

            assertTrue(status is AppIntegrityStatus.Valid)
        }

    @Test
    fun checkIntegrity_returnsSideloaded_whenInstallerIsUntrustedAndNotInDebug() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val checker =
                AndroidAppIntegrityChecker(
                    context = context,
                    isDebug = false,
                    installerNameProvider = { "com.untrusted.sideload.store" },
                )
            val status = checker.checkIntegrity()

            assertTrue(status is AppIntegrityStatus.Sideloaded)
            val reason = (status as AppIntegrityStatus.Sideloaded).reason
            assertTrue(reason.contains("com.untrusted.sideload.store"))
        }

    @Test
    fun checkIntegrity_returnsValid_whenInstallerIsShellAndInDebug() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val checker =
                AndroidAppIntegrityChecker(
                    context = context,
                    isDebug = true,
                    installerNameProvider = { "com.android.shell" },
                )
            val status = checker.checkIntegrity()

            assertTrue(status is AppIntegrityStatus.Valid)
        }

    @Test
    fun checkIntegrity_returnsValid_whenInstallerIsNullAndInDebug() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val checker =
                AndroidAppIntegrityChecker(
                    context = context,
                    isDebug = true,
                    installerNameProvider = { null },
                )
            val status = checker.checkIntegrity()

            assertTrue(status is AppIntegrityStatus.Valid)
        }
}
