package com.payslipmax.pdfparser.ui.theme

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.payslipmax.pdfparser.crypto.ContextHolder
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class AppStringsVersionTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun appVersionIsBrandedPayslipMaxNotPlatformSpecific() {
        // Regression guard: the Settings screen used to hardcode "PayslipMax iOS - Version 1.4.2",
        // wrongly branding a KMP app as iOS-only. This must stay platform-neutral regardless of
        // which platform's actual version number is embedded.
        val packageInfo = PackageInfo().apply { versionName = "9.9.9" }
        val packageManager =
            mockk<PackageManager> {
                every { getPackageInfo("com.payslipmax.pdfparser", 0) } returns packageInfo
            }
        ContextHolder.context =
            mockk<Context> {
                every { packageName } returns "com.payslipmax.pdfparser"
                every { this@mockk.packageManager } returns packageManager
            }

        assertEquals("PayslipMax - Version 9.9.9", AppStrings.appVersion)
    }
}
