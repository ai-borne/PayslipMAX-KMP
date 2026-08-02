package com.payslipmax.pdfparser.platform

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformVersionIosTest {
    @Test
    fun platformAppVersionNeverReturnsBlank() {
        // The xctest host bundle's CFBundleShortVersionString may not match the real app's (it's a
        // different bundle), so this can't assert an exact value — the invariant this proves is that
        // the NSBundle lookup always degrades to the "unknown" fallback rather than an empty string,
        // which is what the Settings screen relies on to never render a blank version line.
        assertTrue(platformAppVersion().isNotBlank())
    }
}
