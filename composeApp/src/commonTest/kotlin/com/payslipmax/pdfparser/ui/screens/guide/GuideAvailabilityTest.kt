package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.subscription.LaunchFlags
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Guide's visibility is the kill switch and nothing else, in every source set and build type. */
class GuideAvailabilityTest {
    @Test
    fun guideVisibilityIsExactlyTheKillSwitch() {
        assertEquals(LaunchFlags.GUIDE_ENABLED, isGuideEnabled())
    }
}
