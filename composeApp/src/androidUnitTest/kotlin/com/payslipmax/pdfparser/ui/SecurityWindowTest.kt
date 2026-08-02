package com.payslipmax.pdfparser.ui

import android.view.WindowManager
import com.payslipmax.pdfparser.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecurityWindowTest {
    @Test
    fun mainActivityAppliesFlagSecureToWindow() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        controller.create()

        val flags = activity.window.attributes.flags
        val isSecureSet = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        assertTrue(isSecureSet, "MainActivity window attributes must contain FLAG_SECURE to prevent screen capture")
    }
}
