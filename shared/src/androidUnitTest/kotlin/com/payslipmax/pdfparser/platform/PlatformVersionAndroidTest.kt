package com.payslipmax.pdfparser.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.payslipmax.pdfparser.crypto.ContextHolder
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class PlatformVersionAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun platformAppVersionReturnsUnknownWithoutAContext() {
        // No real android.content.Context is available in a plain-JVM unit test (ContextHolder.context
        // is never set here) — the invariant this proves is that the Settings screen never crashes or
        // shows a stale/blank string when the context genuinely isn't available yet.
        assertEquals("unknown", platformAppVersion())
    }

    @Test
    fun platformAppVersionReturnsTheRealPackageVersionName() {
        // Guards the SSOT wiring at the runtime boundary: this must read the actual installed
        // build's versionName (sourced from version.properties via composeApp/build.gradle.kts),
        // not a hardcoded string — a regression here would silently reintroduce the stale
        // "PayslipMax iOS - Version 1.4.2" bug this was fixed to prevent.
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

        assertEquals("9.9.9", platformAppVersion())
    }

    @Test
    fun platformAppVersionDegradesToUnknownWhenPackageInfoLookupFails() {
        // PackageManager.getPackageInfo can legitimately throw NameNotFoundException on a real
        // device (e.g. mid-uninstall/reinstall) — this must degrade safely rather than crash the
        // Settings screen.
        val packageManager =
            mockk<PackageManager> {
                every { getPackageInfo("com.payslipmax.pdfparser", 0) } throws
                    PackageManager.NameNotFoundException()
            }
        ContextHolder.context =
            mockk<Context> {
                every { packageName } returns "com.payslipmax.pdfparser"
                every { this@mockk.packageManager } returns packageManager
            }

        assertEquals("unknown", platformAppVersion())
    }
}
