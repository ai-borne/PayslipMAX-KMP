package com.payslipmax.pdfparser.ui.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * WHY: Android's print dialog needs the Activity the officer is looking at, and composition hands over a context that may be wrapped
 * (a theme wrapper, a dialog). Printing from an Application context would crash, so no Activity means no printer target at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SlipPrinterProviderTest {
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).create().get()

    @Test
    fun anActivityIsItsOwnPrintTarget() {
        assertSame(activity, activity.findActivity())
    }

    @Test
    fun anActivityUnderAnyNumberOfWrappersIsFound() {
        assertSame(activity, ContextWrapper(ContextWrapper(activity)).findActivity())
    }

    @Test
    fun anApplicationContextHasNoActivityAndSoNoPrinterTarget() {
        val application: Context = ApplicationProvider.getApplicationContext()

        assertNull(application.findActivity())
        assertNull(ContextWrapper(application).findActivity())
    }
}
