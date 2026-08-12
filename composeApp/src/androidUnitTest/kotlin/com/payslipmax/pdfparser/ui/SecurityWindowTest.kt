package com.payslipmax.pdfparser.ui

import android.view.WindowManager
import com.payslipmax.pdfparser.MainActivity
import com.payslipmax.pdfparser.shouldApplyFlagSecure
import com.payslipmax.pdfparser.subscription.isDebugBuild
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecurityWindowTest {
    @Test
    fun shouldApplyFlagSecureIsTrueForReleaseBuilds() {
        assertTrue(
            shouldApplyFlagSecure(isDebug = false),
            "Shipped (release) builds must block screenshots/screen-recording -- every screen can show real salary/PII data",
        )
    }

    @Test
    fun shouldApplyFlagSecureIsFalseForDebugBuilds() {
        assertFalse(
            shouldApplyFlagSecure(isDebug = true),
            "Debug builds must allow screenshots (adb screencap) so UI changes can be verified during development",
        )
    }

    // This module's `check` gate runs androidUnitTest sources against BOTH the debug and release
    // variants (testDebugUnitTest / testReleaseUnitTest), so this can't hardcode which one is active --
    // it asserts MainActivity's actual wiring matches whatever isDebugBuild() says for the variant
    // this test happens to be running under, proving the real Activity calls through correctly rather
    // than re-testing the policy itself (already covered by the pure tests above).
    @Test
    fun mainActivityFlagSecureMatchesShouldApplyFlagSecurePolicy() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        controller.create()

        val flags = activity.window.attributes.flags
        val isSecureSet = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        assertEquals(shouldApplyFlagSecure(isDebugBuild()), isSecureSet)
    }
}
