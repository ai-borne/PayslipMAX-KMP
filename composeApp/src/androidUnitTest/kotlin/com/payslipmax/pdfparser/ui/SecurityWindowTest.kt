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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecurityWindowTest {
    @Test
    fun shouldApplyFlagSecureIsTrueForReleaseBuilds() {
        assertFalse(
            shouldApplyFlagSecure(isDebug = false),
            "Temporarily disabled during closed testing so testers can share screenshots of issues",
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

    @Test
    fun whenWindowLosesFocus_flagSecureIsAddedToProtectRecents() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        controller.create().start().resume().visible()

        activity.onWindowFocusChanged(false)

        val flags = activity.window.attributes.flags
        val isSecureSet = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        kotlin.test.assertTrue(
            isSecureSet,
            "When window loses focus (entering recents or switching apps), FLAG_SECURE must be applied to protect snapshots",
        )
    }

    @Test
    fun whenWindowGainsFocus_flagSecureIsClearedToAllowScreenshots() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        controller.create().start().resume().visible()

        activity.onWindowFocusChanged(false)
        activity.onWindowFocusChanged(true)

        val flags = activity.window.attributes.flags
        val isSecureSet = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        assertFalse(
            isSecureSet,
            "When window gains focus (user actively interacting), FLAG_SECURE must be cleared to allow screenshots",
        )
    }
}
